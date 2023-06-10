(ns new-todo-bot.courses.views
  (:require [clojure.core.async :refer [<! go timeout]]
            [clojure.string :as s]
            [clojure.walk :refer [keywordize-keys]]
            [morse.api :as t]
            [new-todo-bot.chatgpt.prompts :as ps]
            [new-todo-bot.common.utils :as u]
            [new-todo-bot.config :refer [gpt-token token]]
            [new-todo-bot.courses.controllers :as c]
            [new-todo-bot.courses.keyboards :refer [build-node-buttons]]
            [new-todo-bot.db.helpers.common :refer [get-by]]
            [new-todo-bot.db.helpers.constants :as const]
            [new-todo-bot.db.helpers.course-elements :refer [TCourseElements]]
            [new-todo-bot.db.helpers.documents :refer [TDocuments]]
            [new-todo-bot.db.helpers.user-last-course :refer [get-last-course get-last-course-desc get-next-course-element]]
            [new-todo-bot.db.helpers.user-progress :refer [create-user-course!]]
            [new-todo-bot.db.helpers.users :refer [ensure-chat-exists! ensure-user-exists!]]
            [new-todo-bot.telegram.keyboards :as kb]
            [new-todo-bot.telegram.senders :as ts]
            [ring.util.codec :refer [form-decode form-encode]]
            [wkok.openai-clojure.api :as gpt-api]
            [new-todo-bot.telegram.utils :as tu]))

;; Commands

(defn list-
  "Отправляет пользователю список доступных курсов"
  [{chat :chat}]
  (let [courses (take 20 (c/get-courses-list))
        buttons (build-node-buttons courses "get_course")]
    (ts/send-keyboard token (:id chat) "Список доступных курсов: ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀" buttons)))

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

(defn ask-mentor
  "Отправить вопрос к chatgpt"
  [{:keys [text from] {id :id} :chat :as message}]
  (if (seq text)
    (let [{:keys [display_name author]} (get-last-course-desc (:id from))
          r (gpt-api/create-chat-completion
              {:model    "gpt-3.5-turbo"
               :messages [{:role "system" :content (format ps/course display_name author)}
                          {:role "user" :content text}]}
              {:api-key gpt-token})
          r-text (-> r :choices first :message :content)]
      (t/send-text token id r-text))
    (t/send-text token id "you sent nothing")))

;; Callbacks

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

(defn get-course
  "Возвращает структуру курса и его описание"
  [{:keys [] {:keys [chat]} :message} {id- :id}]
  (let [id (Integer/parseInt id-)
        course (c/get-course-information id)
        {:keys [author display_name source_url description]} course
        course-structure-col (c/build-course-structure id)
        course-structure-text (reduce into [] (map c/render-course-structure course-structure-col))
        structure (s/join "\n" course-structure-text)
        text (str "Курс: " (tu/escape-md2 display_name) "\n"
                  (when author (str "Автор: " (tu/escape-md2 author) "\n"))
                  (when source_url (str "Источник: " (u/md-link source_url "YouTube") "\n"))
                  (when description (str "Описание: " description "\n\n"))
                  (tu/escape-md2 structure))
        ;; Ограничение в tg bot api больше ~4000 символов нельзя
        cropped-text* (if (> (count text) 4000)
              (-> text
                  (subs 0 4000)
                  (s/split #"\n")
                  (drop-last)
                  (#(s/join "\n" %))
                  (str (tu/escape-md2 "\n...")))
              text)
        start-course-button (kb/single-button-kb "Начать изучение" (form-encode {:url "start_course" :id id}))]
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
        documents (get-by TDocuments where-cond :order-by :sort)
        documents-grouped (group-by :type documents)]
    (when-let [videos (get documents-grouped "external-video")]
      (->> (map #(str (u/md-link (:url %) (:display_name %))) videos)
          (s/join "\n")
          (t/send-text token (:id chat) {:parse_mode "MarkdownV2"})))
    (go (doseq [doc (apply concat (vals (select-keys documents-grouped ["audio" "file"])))]
          (t/send-document token (:id chat) (:tg_file_id doc))
          (<! (timeout 1000))))))

(comment
  (defn crop-bytes [s n]
    (let [bytes (byte-array n)]
      (System/arraycopy (.getBytes s) 0 bytes 0 (min n (.length s)))
      (new String bytes "UTF-8")))

  (defn get-line [s byte-offset]
    (let [line-number (count (clojure.string/split-lines (crop-bytes s byte-offset)))
          llll (clojure.string/split-lines s)]
      (get llll (dec line-number)))))