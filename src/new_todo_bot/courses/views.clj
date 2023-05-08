(ns new-todo-bot.courses.views
  (:require [new-todo-bot.courses.controllers :as c]
            [new-todo-bot.telegram.keyboards :as kb]
            [new-todo-bot.telegram.senders :as ts]
            [clojure.string :as s]
            [ring.util.codec :refer [form-encode form-decode]]
            [clojure.walk :refer [keywordize-keys]]
            [new-todo-bot.db.helpers.user-progress :refer [create-user-course!]]
            [new-todo-bot.db.helpers.common :refer [get-by]]
            [new-todo-bot.db.helpers.users :refer [ensure-user-exists! ensure-chat-exists!]]
            [morse.api :as t]
            [new-todo-bot.db.helpers.constants :as const]
            [new-todo-bot.courses.keyboards :refer [build-node-buttons build-course-kb new-dir-up-button]]
            [new-todo-bot.db.helpers.course-elements :refer [TCourseElements]]
            [new-todo-bot.db.helpers.documents :refer [TDocuments]]
            [environ.core :refer [env]]
            [new-todo-bot.common.utils :as u]
            [clojure.core.async :refer [<! timeout go]]
            [new-todo-bot.db.helpers.user-last-course :refer [get-last-course get-next-course-element]]))

(def token (env :telegram-token))

(defn list-
  "Отправляет пользователю список доступных курсов"
  [{chat :chat}]
  (let [courses (take 20 (c/get-courses-list))
        buttons (build-node-buttons courses "get_course")]
    (ts/send-keyboard token (:id chat) "Список курсов: " buttons)))

(defn start
  [{chat :chat user :from}]
  (ensure-user-exists! user)
  (ensure-chat-exists! chat)
  (t/send-text token (:id chat) "Для вывода списка курсов напишите команду /list"))

(defn get-last-requested-course
  "Отправка последнего запрошенного курса"
  [{chat :chat user :from}]
  (let [{:keys [element_id element_type]} (first (get-last-course (:id user)))]
    (c/get-item- token (:id chat) element_id element_type)))

(defn get-next-course
  "Отправка последнего запрошенного курса"
  [{chat :chat user :from}]
  (if-let [{:keys [element_id element_type]} (first (get-next-course-element (:id user)))]
    (c/get&save-item-for-user token (:id chat) element_id element_type)
    (t/send-text token (:id chat) "Это последний урок курса")))

(defn defcallback
  "Регистрация колбэков и предобработка сообщения. В будущем
  здесь могут появиться middleware."
  [& callbacks]
  (fn [message]
    (if-let [data (:data message)]
      (let [data (keywordize-keys (form-decode data))
            url (keyword (:url data))
            callbacks (apply hash-map callbacks)
            handler (get callbacks url)]
        (println "got callback\n" data)
        (handler message data))
      message)))

(comment

  (let [{:keys [author display_name source_url description]} {:id 1,
                                                                :name "Effortless English - New method learning english",
                                                                :display_name "Effortless English - New method learning english",
                                                                :author nil,
                                                                :source nil,
                                                                :description nil,
                                                                :source_url nil,
                                                                :created_at nil} ]
    (println author display_name source_url description)))

;; callbacks
(defn get-course
  "Возвращает структуру курса и его описание"
  [{:keys [] {:keys [chat]} :message} {id- :id}]
  (let [id (Integer/parseInt id-)
        course (c/get-course-information id)
        {:keys [author display_name source_url description]} course
        course-structure-col (c/build-course-structure id)
        course-structure-text (reduce into [] (map c/render-course-structure course-structure-col))
        structure (s/join "\n" course-structure-text)
        text (str "Курс: " display_name "\n"
                  (when author (str "Автор: " author "\n" ))
                  (when source_url (str "Источник: " source_url "\n"))
                  (when description (str "Описание: " description "\n\n"))
                  structure)
        ;; Ограничение в tg bot api больше ~4000 символов нельзя
        cropped-text (subs text 0 (min (count text) 4000))
        cropped-text* (if (> (count text) 4000)
                        (str cropped-text "\n...")
                        cropped-text)
        start-course-button (kb/single-button-kb "Начать" (form-encode {:url "start_course" :id id}))]
    (ts/send-keyboard token (:id chat) cropped-text* start-course-button)))

(defn start-course
  "Создает для пользователя запись прогресса, возвращает
  содержание первой директории курса."
  [{:keys [] {:keys [chat from]} :message} {id :id}]
  (let [id (Integer/parseInt id)
        user (first (get-by [:users :id] {:telegram_id (:id chat)}))]
    (create-user-course! id (:id user))
    (c/get&save-item-for-user token (:id chat) id const/course-type)))

(def edit-keyboard
  (fn
    [message-id token chat-id text keyboard]
    (ts/edit-message token chat-id message-id text keyboard)))

(defn get-item
  "Отправляет элемент или содержание директории"
  [{:keys [] {:keys [chat message_id]} :message} {:keys [id type page-number page-size]}]
  (let [send-keyboard (partial edit-keyboard message_id)]
    (c/get&save-item-for-user
      token
      (:id chat)
      id
      type
      :page-number page-number
      :page-size page-size
      :send-keyboard send-keyboard)))
(defmacro if-let*
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(drop 2 bindings) ~then ~else)
        ~else)
     then)))

(defn get-dir-above
  "Отправить содержание родительской директории"
  [{:keys [] {:keys [chat message_id]} :message} {id :id _ :type}]
  (let [send-keyboard (partial edit-keyboard message_id)]
    (if-let [element (first (get-by TCourseElements {:id (u/parse-int id)}))]
      (if-let [parent-id (-> element :parent_id u/parse-int)]
        (c/get&save-item-for-user token (:id chat) parent-id const/element-type :send-keyboard send-keyboard)
        (let [course-id (:course_id element)]
          (c/get&save-item-for-user token (:id chat) course-id const/course-type :send-keyboard send-keyboard)))
      (t/send-text token (:id chat) "Элемент не найден"))))

(defn get-course-files
  "Отправка нескольких файлов курса"
  [{:keys [] {:keys [chat]} :message} {:keys [parent-id course-id]}]
  (let [[parent-id course-id] [(u/parse-int parent-id) (u/parse-int course-id)]
        where-cond (merge {:course-element-id parent-id}
                          (when course-id {:course-id course-id}))
        documents (get-by TDocuments where-cond)]
    (go (doseq [doc documents]
          (t/send-document token (:id chat) (:tg_file_id doc))
          (<! (timeout 1000))))))

(comment
  (ts/send-keyboard token 37521589 "asdf" a)
  (new-dir-up-button 10)
  (build-course-kb :course-id 1 :parent-id 10)
  (start-course {:message {:chat {:id 37521589}}} {:id 1})
  (:tg_file_id (first (get-by TDocuments {:id 1})))

  (get-item {:message {:chat {:id 37521589}}} {:id 1 :type const/element-type})
  (case "documents"
    const/document-type (-> (get-by TDocuments {:telegram_id id})
                            first
                            (t/send-document
                              token
                              (:id chat)))
    const/element-type (ts/send-keyboard
                         token
                         (:id chat)
                         "Список курсов"
                         (build-course-kb :parent-id id)))
  (get-course-files {:message {:chat {:id 37521589}}}
                    {:parent-id 56 :course-id ""})
  (get-by TDocuments (merge {:course-element-id (u/parse-int 56)}
                            (when (u/parse-int "")
                              {:course-id (u/parse-int "")})))
  (get-dir-above
    {:message {:chat {:id 37521589}}} {:id 3 :type ""}))

