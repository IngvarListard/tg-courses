(ns new-todo-bot.db.helpers.course-elements
  (:require [toucan.models :refer [defmodel, IModel]]
            [new-todo-bot.db.helpers.common :refer [get-by build-where-cond]]
            [new-todo-bot.db.helpers.documents :refer [TDocuments]]
            [new-todo-bot.db.helpers.constants :as const]
            [new-todo-bot.db.conn :refer [db]]))

(defmodel CourseElements :course_elements IModel)

(def ^:const TCourseElements :course_elements)

(defn get-course-elements
  "Получить элементы и документы привязанные к курсу.
  Если course-id и/или parent-id указаны, то фильтрует по
  этим значениям. Значения могут быть nil"
  [& {:keys [course-id parent-id] :as args}]
  (let [prepare-cond #(reduce
                        (fn [init [k v]]
                          (if (contains? args v)
                            (into init {k (v args)})
                            init))
                        {} %)
        elements (get-by TCourseElements
                         (prepare-cond {:parent_id :parent-id :course_id :course-id})
                         :order-by [:sort])
        documents (get-by TDocuments
                          (prepare-cond {:course_element_id :parent-id :course_id :course-id})
                          :order-by [:sort])]
    [elements documents]))

(defn get-course-content
  [& {:keys [course-id parent-id offset limit return-count?] :as args}]
  (let [where (build-where-cond (select-keys args [:course-id :parent-id]))
        select (if return-count? [:%count.*] [:id :display_name :type :entity])
        sqlmap (merge {:select select
                       :from   [[{:union-all [{:select [:id
                                                        :display_name
                                                        :course_id
                                                        :parent_id
                                                        [1 :typed_order]
                                                        [const/folder-type :type]
                                                        [const/element-type :entity]
                                                        :sort]
                                               :from   :course_elements}
                                              {:select [:id
                                                        :display_name
                                                        :course_id
                                                        [:course_element_id :parent_id]
                                                        [1 :typed_order]
                                                        :type
                                                        [const/document-type :entity]
                                                        [nil :sort]]
                                               :from   :documents}]} :d]]
                       :where  where}
                      (when (not return-count?) {:order-by [:typed_order :sort]})
                      (when offset {:offset offset})
                      (when limit {:limit limit}))
        result (db :execute! sqlmap)]
    (if return-count? (first result) result)))

(defn get-element-display-name
  [element-id]
  (let [element (db :execute! {:select [:display_name]
                               :from   TCourseElements
                               :where  [:= :id {:select [:parent-id]
                                                :from   TCourseElements
                                                :where  [:= :id element-id]}]})
        display-name (-> element first :display_name)]
    display-name))
