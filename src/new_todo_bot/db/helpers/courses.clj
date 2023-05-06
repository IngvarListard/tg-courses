(ns new-todo-bot.db.helpers.courses
  (:require [toucan.models :refer [defmodel, IModel]]
            [new-todo-bot.db.helpers.common :refer [get-by]]))

(defmodel Courses :courses IModel)

(def ^:const TCourses :courses)

(defn get-course-display-name
  [course-id]
  (first (get-by TCourses {:id course-id})))
