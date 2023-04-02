(ns new-todo-bot.todos.core
  (:require [new-todo-bot.db.todos :as db-todo]))

(defn add-todo!
  [uid text]
  (db-todo/add-todo ))

(defn done-todo!
  [todo-id])

(defn delete-todo!
  [todo-id])
