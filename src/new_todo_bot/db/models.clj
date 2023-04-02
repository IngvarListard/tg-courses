(ns new-todo-bot.db.models
  (:require [toucan.models :refer [defmodel, IModel]]
            [toucan.db :as db]
            [new-todo-bot.db.conn :refer [db-connection]]))


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

(defn ensure-user-exists! [user]
  (let [uid (:id user)
        new-user (-> user
                     (select-keys [:first_name :last_name :username])
                     (assoc :telegram_id uid))]
    (if (and (some? uid) (db/exists? 'User {:telegram_id uid}))
         uid
         (:id (db/insert! 'User new-user)))))
(defn insert!
  [model obj]
  )

(defmodel Todo :todos IModel)

(def get-todos-by (partial get-by 'Todo))

(defmodel User :users IModel)

(def get-users-by (partial get-by 'User))

(defmodel Chat :chats IModel)

(defn ensure-chat-exists! [chat]
  (let [chat-id (:id chat)
        new-chat (-> chat
                     (select-keys [:type])
                     (assoc :telegram_id chat-id))]
    (if (and (some? chat-id) (db/exists? 'Chat {:telegram_id chat-id}))
      chat-id
      (:id (db/insert! 'Chat new-chat)))))

(def get-chats-by (partial get-by 'Chat))

(comment
  (add-todo (db-connection) {:description "ok" :status "ok"})
  (todo-by-id (db-connection) {:id 2})
  (todos-by-ids-specify-cols (db-connection) {:ids [1 2] :cols ["id" "status"]})
  ()
  )
