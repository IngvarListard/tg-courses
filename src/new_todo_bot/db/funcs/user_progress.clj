(ns new-todo-bot.db.funcs.user-progress
  (:require [toucan.models :refer [defmodel, IModel]]
            [new-todo-bot.db.conn :refer [db]]
            [new-todo-bot.db.funcs.common :refer [get-by insert-into!]]))

(defmodel UserProgress :user_progress IModel)

(defmodel UserCourse :user_course IModel)

(defn create-user-course!
  "Create user_course record in database"
  [course-id user-id]
  (let [course (first (get-by :user-course {:user-id user-id :course-id course-id}))]
    (if (empty? course)
      (insert-into! :user_course {:user_id   user-id
                                  :course_id course-id
                                  :status    "STARTED"})
      course)))

(defn set-user-progress!
  "Update or create user progress record with specific status"
  [course-id user-id element-id user-course-id type- status]
  (let [user-progress (get-by [:user_progress :id]
                              {:course-id course-id
                               :user-id user-id
                               :element-id element-id
                               :element-type type-})]
    (if user-progress
      (db :execute
          {:update :user-progress
           :set {:status status}
           :where [:= :id (:id (first user-progress))]})
      (insert-into! :user-progress
                    {:user-id user-id
                     :course-id course-id
                     :element-id element-id
                     :element-type type-
                     :status status
                     :user-course-id user-course-id}))))

(defn get-user-progress
  [user-id]
  (db :execute! {:select [[:course_elements.name :element_name]
                      [:documents.name :document_name]
                      [:user_progress.status :status]]
             :from :user_progress
             :left-join [:course_elements [:and
                                           [:= :user_progress.element_id :course_elements.id]
                                           [:= :user_progress.element_type "course_element"]]
                         :documents [:and
                                     [:= :user_progress.element_id :documents.id]
                                     [:= :user_progress.element_type "document"]]]
             :where [:= :user_progress.user_id user-id]}))
