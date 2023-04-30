(ns new-todo-bot.db.conn
  (:require [next.jdbc :as sql]
            [honey.sql :as hsql]
            [toucan.db :as -db]
            [next.jdbc.result-set :as rs]
            [environ.core :refer [env]])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))

(let [host (env :postgres-host)
      port (env :postgres-port)
      user (env :postgres-user)
      password (env :postgres-password)
      database (env :postgres-database)]
  (def db-spec {
                :classname "org.postgresql.Driver"
                :subprotocol "postgresql"
                :subname (format "//%s:%s/%s" host port database)
                :user user
                :password password
                :create true}))


;; connections pooling
(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(def pooled-db (delay (pool db-spec)))

(defn db-connection [] @pooled-db)

(-db/set-default-db-connection! (db-connection))

;; Установка по-умолчанию чтобы nextjs не возвращал префиксы
(def ds-opts (sql/with-options (db-connection) {:builder-fn rs/as-unqualified-lower-maps}))

;; TODO debug log all formatted queries
(defn db
  "Общая функция для выполнения запросов к БД"
  ([action data-map] (db action data-map {}))
  ([action data-map opts]
   (let [jdbc-func (resolve (symbol (str "next.jdbc/" (name action))))
         raw-sql (hsql/format data-map)]
     (jdbc-func ds-opts raw-sql opts))))
