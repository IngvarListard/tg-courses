(ns new-todo-bot.db.helpers.users
  (:require [toucan.db :as db]
            [toucan.models :refer [defmodel, IModel]]))

(defn get-by
  "Function that helps get records from DB for any model"
  ([model] (get-by model nil nil))
  ([model params] (get-by model params nil))
  ([model params fields]
   (let [model (if
                 (nil? fields)
                 model
                 (into [model] fields))]
     (apply db/select model (mapcat seq params)))))

(defmodel User :users IModel)

(defn ensure-user-exists!
  "Проверить что пользователь существует либо создать его"
  [user]
  (let [uid (:id user)
        new-user (-> user
                     (select-keys [:first_name :last_name :username])
                     (assoc :telegram_id uid))]
    (println uid)
    (if (and (some? uid) (db/exists? 'User {:telegram_id uid}))
      uid
      (:id (db/insert! 'User new-user)))))

(defmodel Chat :chats IModel)

(defn ensure-chat-exists!
  "Проверить что пользователь существует либо создать его"
  [chat]
  (let [chat-id (:id chat)
        new-chat (-> chat
                     (select-keys [:type])
                     (assoc :telegram_id chat-id))]
    (if (and (some? chat-id) (db/exists? 'Chat {:telegram_id chat-id}))
      chat-id
      (:id (db/insert! 'Chat new-chat)))))
