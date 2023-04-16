(ns new-todo-bot.db.helpers.course-elements
  (:require [toucan.models :refer [defmodel, IModel]]
            [new-todo-bot.db.helpers.common :refer [get-by]]
            [new-todo-bot.db.helpers.documents :refer [TDocuments]]))

(defmodel CourseElements :course_elements IModel)

(defonce TCourseElements :course_elements)

(defn get-course-elements
  "Получить элементы и документы привязанные к курсу"
  [& {:keys [course-id parent-id] :as args}]
  (println args)
  (let [prepare-cond #(reduce
                        (fn [init [k v]]
                          (if (contains? args v)
                            (into init {k (v args)})
                            init))
                        {} %)
        elements (get-by TCourseElements (prepare-cond {:parent_id :parent-id :course_id :course-id}))
        documents (get-by TDocuments (prepare-cond {:course_element_id :parent-id :course_id :course-id}))]
    [elements documents]))