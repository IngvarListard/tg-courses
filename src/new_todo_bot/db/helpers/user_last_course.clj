(ns new-todo-bot.db.helpers.user-last-course
  (:require [new-todo-bot.db.helpers.common :refer [update-by! insert-into! get-by]]
            [new-todo-bot.db.helpers.constants :as const]
            [toucan.db :as db]
            [new-todo-bot.db.conn :refer [db]]
            [clojure.string :as s]))

(defn dfield
  [& args]
  (keyword (s/join "." (map name args))))
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

(comment
  )