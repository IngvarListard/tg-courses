(ns new-todo-bot.db.conn
  (:require [clojure.java.jdbc :as sql])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))

(def db-spec {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname "resources/db/db.sqlite3"
              :create true})

;; connections pooling
(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               ;;(.setUser (:user spec))
               ;;(.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(def pooled-db (delay (pool db-spec)))

(defn db-connection [] @pooled-db)

(comment


  (sql/query (db-connection) ["select * from todos"])

  )
