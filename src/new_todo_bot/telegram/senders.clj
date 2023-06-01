(ns new-todo-bot.telegram.senders
  (:require [clj-http.client :as http]))

(def ^:const base-url "https://api.telegram.org/bot")

(defn send-message
  ([token message] (send-message token message "/sendMessage"))
  ([token message url]
   (let [url (str base-url token url)
         body message
         resp (http/post url {:content-type :json
                              :as :json
                              :form-params body})]
     (-> resp :body))))

(defn send-keyboard
  "Sends inline keyboard"
  ([token chat-id text keyboard] (send-keyboard token chat-id text keyboard {}))
  ([token chat-id text keyboard options]
   (let [message (into {:chat_id chat-id
                        :text text
                        :reply_markup {:inline_keyboard keyboard}
                        :parse_mode "markdown"}
                       options)]
     (send-message token message))))

(defn edit-keyboard
  [token chat-id message-id keyboard]
  (let [message (into {:chat_id chat-id
                       :message_id message-id
                       :reply_markup {:inline_keyboard keyboard}})]
    (send-message token message "/editMessageReplyMarkup")))

(defn edit-message
  [token chat-id message-id text keyboard]
  (let [message (into {:chat_id chat-id
                       :message_id message-id
                       :text text
                       :reply_markup {:inline_keyboard keyboard}
                       :parse_mode "markdown"})]
    (send-message token message "/editMessageText")))

(defn send-menu
  "Sends reply keyboard"
  ([token chat-id text keyboard] (send-menu token chat-id text keyboard {}))
  ([token chat-id text keyboard options]
   (let [url (str base-url token "/sendMessage")
         body (into {:chat_id chat-id
                     :text text
                     :reply_markup {:keyboard keyboard}
                     :parse_mode "markdown"} options)
         resp (http/post url {:content-type :json
                              :as :json
                              :form-params body})]
     (-> resp :body))))
