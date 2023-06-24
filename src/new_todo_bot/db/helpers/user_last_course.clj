(ns new-todo-bot.db.helpers.user-last-course
  (:require [new-todo-bot.db.helpers.common :refer [update-by! insert-into! get-by]]
            [new-todo-bot.db.helpers.constants :as const]
            [toucan.db :as db]
            [new-todo-bot.db.conn :refer [db]]
            [new-todo-bot.db.helpers.courses :refer [TCourses]]))

(def ^:const TUserLastCourse :user_last_course)
(defn update-user-last-course!
  "Обновить последний запрошенный пользователем курс,
  либо вставить новую запись"
  [user-id element-id element-type]
  (db/transaction
    (when (and element-id element-type)
      (when (empty? (update-by! [TUserLastCourse :id]
                                {:user-id user-id}
                                {:user-id      user-id
                                 :element-id   element-id
                                 :element-type element-type}))
        (insert-into! [TUserLastCourse :id]
                      {:user-id      user-id
                       :element-id   element-id
                       :element-type element-type})))))

(defn get-last-course
  [tg-user-id]
  (db :execute! {:select [:element-id :element-type]
                 :from   TUserLastCourse
                 :join   [:users [:= :user_last_course.user_id :users.id]]
                 :where  [:= :telegram_id tg-user-id]}))

(defn get-course-display-name-by-element-id
  [el-id]
  (db :execute! {:select [:courses.id :courses.display_name]
                 :from   TCourses
                 :join   [:course_elements [:= :courses.id :course_elements.course_id]]
                 :where  [:= :course_elements.id el-id]}))

(defn get-last-course-desc
    [tg-user-id]
    (when-let [{:keys [element_id element_type]} (-> tg-user-id get-last-course first)]
      (let [ks [:author :display_name]]
        (println element_type)
        (condp = element_type
          const/course-type (->> {:id element_id} (get-by TCourses) first (select-keys ks))
          const/element-type (->> element_id get-course-display-name-by-element-id first (select-keys ks))))))

(defmulti get-next-course
          (fn [_ element-id] element-id))

(defmethod get-next-course const/element-type
  [element-id element-type]
  (let [element-type* (keyword element-type)]
    (db :execute! {:select   [[:id :element-id] [element-type :element-type]]
                   :from     element-type*
                   :where    [:and [:> :sort {:select [:sort]
                                              :from   element-type*
                                              :where  [:= :id element-id]}]
                              [:= :course-id {:select [:course-id]
                                              :from   element-type*
                                              :where  [:= :id element-id]}]]
                   :order-by [:sort]
                   :limit    1})))

(defmethod get-next-course const/course-type
  [element_id element_type]
  [{:element_id element_id :element_type element_type}])

(defn get-next-course-element
  [tg-user-id]
  (let [[{:keys [element_id element_type]}] (get-last-course tg-user-id)]
    (println element_type element_id)
    (get-next-course element_id element_type)))
