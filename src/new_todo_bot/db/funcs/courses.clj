(ns new-todo-bot.db.funcs.courses
  (:require [toucan.models :refer [defmodel, IModel]]))

(defmodel Courses :courses IModel)
