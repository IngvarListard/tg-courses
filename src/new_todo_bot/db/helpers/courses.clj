(ns new-todo-bot.db.helpers.courses
  (:require [toucan.models :refer [defmodel, IModel]]))

(defmodel Courses :courses IModel)

(def ^:const TCourses :courses)
