(ns new-todo-bot.courses.views
  (:require [new-todo-bot.courses.controllers :as c]
            [new-todo-bot.telegram.keyboards :as kb]
            [new-todo-bot.telegram.senders :as ts]
            [clojure.string :as s]
            [ring.util.codec :refer [form-encode form-decode]]
            [clojure.walk :refer [keywordize-keys]]
            [new-todo-bot.db.funcs.user-progress :refer [create-user-course!]]
            [new-todo-bot.db.funcs.common :refer [get-by]]
            [new-todo-bot.db.funcs.users :refer [ensure-user-exists! ensure-chat-exists!]]
            [morse.api :as t]))

(def token "")

; TODO: пагинация
(defn list-
  [{chat :chat}]
  (let [courses (take 20 (c/get-courses-list))
        buttons (map #(kb/new-button (:display_name %) (form-encode {:url "get_course" :id (:id %)})) courses)
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
    (ts/send-keyboard token (:id chat) text start-course-button)) {:parse_mode "Markdown"})

(defn start-course
  [{:keys [] {:keys [chat from]} :message} {id :id}]
  (let [user (first (get-by [:users :id] {:telegram_id (:id chat)}))]
    (println "imhere")
    (create-user-course! id (:users/id user))))

