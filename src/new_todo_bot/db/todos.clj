(ns new-todo-bot.db.todos
  (:require [hugsql.core :as hugsql]
            [new-todo-bot.db.conn :refer [db-connection]]))

;; The path is relative to the classpath (not proj dir!),
;; so "src" is not included in the path.
;; The same would apply if the sql was under "resources/..."
;; Also, notice the under_scored path compliant with
;; Clojure file paths for hyphenated namespaces
(hugsql/def-db-fns "new_todo_bot/db/sql/todos.sql")

;; for advanced usage with database functions.
(hugsql/def-sqlvec-fns "new_todo_bot/db/sql/todos.sql")


(comment
    (add-todo (db-connection) {:description "ok" :status "ok"})
    (todo-by-id (db-connection) {:id 2})
    (todos-by-ids-specify-cols (db-connection) {:ids [1 2] :cols ["id" "status"]})
  )
