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
            [new-todo-bot.courses.keyboards :refer [build-node-buttons build-course-kb]]
            [new-todo-bot.db.helpers.documents :refer [TDocuments]]))

(def token "6022297989:AAFsZ9UT34wg_8fcsIdAThxZ1aebvdHDRNQ")

; TODO: пагинация
(defn list-
  [{chat :chat}]
  (let [courses (take 20 (c/get-courses-list))
        buttons (build-node-buttons courses :display_name :id "get_course")
        lines (map kb/new-line buttons)]
    (println lines (:id chat))
    (ts/send-keyboard token (:id chat) "Список курсов" lines)))

(defn start
  [{chat :chat user :from}]
  (ensure-user-exists! user)
  (ensure-chat-exists! chat)
  (t/send-text token (:id chat) "Well cum"))

(defn defcallback [& callbacks]
  (fn [message]
    (let [data (keywordize-keys (form-decode (:data message)))
          url (keyword (:url data))
          callbacks (apply hash-map callbacks)
          handler (get callbacks url)]
      (handler message data))))

;; callbacks
(defn get-course
  [{:keys [] {:keys [chat]} :message} {id :id}]
  (let [course (c/get-course-information id)
        course-structure-map (c/build-course-structure id)
        course-structure-text (c/render-course-structure course-structure-map "")
        structure (s/join "\n" course-structure-text)
        text (str "Курс: " (:display_name course) "\n"
                  "Автор: " (:author course) "\n"
                  "Источник: " (:source_url course) "\n"
                  "Описание: " (:description course) "\n\n"
                  structure)
        start-course-button (kb/single-button-kb "Начать" (form-encode {:url "start_course" :id id}))]
    (ts/send-keyboard token (:id chat) text start-course-button)))

;; TODO пагинация
(defn start-course
  [{:keys [] {:keys [chat]} :message} {id :id}]
  (let [user (first (get-by [:users :id] {:telegram_id (:id chat)}))
        lines (build-course-kb :course-id id :parent-id nil)
        text "Выберите элемент курса:\n\n"]
    (create-user-course! id (:id user))
    (ts/send-keyboard token (:id chat) text lines)))

(defn get-item [{:keys [] {:keys [chat]} :message} {id :id type- :type}]
  (condp = type-
    const/document-type (->> (get-by TDocuments {:id id})
                            first
                            :tg_file_id
                             (t/send-document
                               token
                               (:id chat)))
    const/element-type (ts/send-keyboard
                         token
                         (:id chat)
                         "Список курсов"
                         (build-course-kb :parent-id id))))

(comment
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
                         (build-course-kb :parent-id id))))

