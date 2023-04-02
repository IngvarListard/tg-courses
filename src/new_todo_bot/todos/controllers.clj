(ns new-todo-bot.todos.controllers
  (:require [morse.api :as t]
            [new-todo-bot.tg-api.senders :as s]
            [clojure.string :as str]
            [new-todo-bot.db.models :as models]
            [new-todo-bot.db.models :refer [Todo]]
            [clojure.pprint :refer [pprint]]
            [toucan.db :as db]))

(def token "6022297989:AAFsZ9UT34wg_8fcsIdAThxZ1aebvdHDRNQ")

(defn start
  [{chat :chat user :from}]
  (models/ensure-user-exists! user)
  (models/ensure-chat-exists! chat)

  (println "Bot joined new chat: " chat)
  (t/send-text token (:id chat) "Welcome to new-todo-bot!"))

(defn help
  [{{id :id :as chat} :chat}]
    (println "Help was requested in " chat)
    (t/send-text token id "Help is on the way"))

(defn list- [{chat :chat user :from}]
  (let [todos (db/select 'Todo)]
    (println "List command from" (str/join " " (vals (select-keys user [:first_name :last_name]))))
    (->> todos
         (map (fn [el] (let [status (case (:status el)
                                       "todo" " ☐\uFE0F "
                                       "done" " ☑\uFE0F "
                                       (str " " (:status el) " "))]
                          (str "/done_" (:id el) status (:description el)))))
         (str/join "\n")
         (t/send-text token (:id chat)))))

(defn todo [{chat :chat text :text user :from}]
  (db/insert! 'Todo {:description (str/replace text #"/todo " "") :status "todo"})
  ; (t/send-text token (:id all) "Help is on the way")
  )

(defn done [{chat :chat text :text user :from}]
  (let [todo-id (last (str/split text #"_"))]
    (db/update-where! 'Todo {:id todo-id} {:status "done"})))

(defn default [{{id :id} :chat :as message}]
  (println "Intercepted message: " message)
  (t/send-text token id "Ok"))