(ns new-todo-bot.tg-api.senders
  (:require [clj-http.client :as http]
            [clojure.string :as string]
            [new-todo-bot.tg-api.core :as kb]))


(def base-url "https://api.telegram.org/bot")

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
  ([token chat-id text] (send-keyboard token chat-id {} text))
  ([token chat-id options text]
   (let [message (into {:chat_id chat-id
                        :text text
                        :reply_markup {:inline_keyboard (kb/new-keyboard)}}
                       options)]
     (send-message token message))))


(defn send-menu
  "Sends reply keyboard"
  ([token chat-id] (send-menu token chat-id {}))
  ([token chat-id options]
   (let [url (str base-url token "/sendMessage")
         body (into {:chat_id chat-id
                     :text "test"
                     :reply_markup {:keyboard [["asdf" "qwer" "2134"]]}} options)
         resp (http/post url {:content-type :json
                              :as :json
                              :form-params body})]
     (-> resp :body))))
