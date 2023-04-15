(ns new-todo-bot.db.funcs.course-elements
  (:require [toucan.models :refer [defmodel, IModel]]))

(defmodel CourseElements :course_elements IModel)
