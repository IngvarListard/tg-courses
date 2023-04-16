(ns new-todo-bot.telegram.senders
  (:require [clj-http.client :as http]))

(def ^:const base-url "https://api.telegram.org/bot")

(defn send-message
  [token message]
  (let [url (str base-url token "/sendMessage")
         body message
         resp (http/post url {:content-type :json
                              :as :json
                              :form-params body})]
     (-> resp :body)))

(defn send-keyboard
  "Sends inline keyboard"
  ([token chat-id text keyboard] (send-keyboard token chat-id text keyboard {}))
  ([token chat-id text keyboard options]
   (let [message (into {:chat_id chat-id
                        :text text
                        :reply_markup {:inline_keyboard keyboard}}
                       options)]
     (send-message token message))))

(defn send-menu
  "Sends reply keyboard"
  ([token chat-id text keyboard] (send-menu token chat-id text keyboard {}))
  ([token chat-id text keyboard options]
   (let [url (str base-url token "/sendMessage")
         body (into {:chat_id chat-id
                     :text text
                     :reply_markup {:keyboard keyboard}} options)
         resp (http/post url {:content-type :json
                              :as :json
                              :form-params body})]
     (-> resp :body))))
