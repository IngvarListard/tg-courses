(ns new-todo-bot.courses.controllers
  (:require [clojure.string :as s]
            [new-todo-bot.db.helpers.course-elements :refer [CourseElements]]
            [new-todo-bot.db.helpers.courses :refer [Courses]]
            [new-todo-bot.db.helpers.documents :refer [Documents]]
            [toucan.db :as db]
            [new-todo-bot.db.helpers.common :refer [get-by]]
            [new-todo-bot.db.helpers.course-elements :refer [TCourseElements]]
            [new-todo-bot.db.helpers.documents :refer [TDocuments]]))

(def ^:const icons
  {"file" "\uD83D\uDCC4 "
   "folder" "\uD83D\uDCC1 "
   "audio" "\uD83D\uDD0A "})

(defn get-courses-list
  "Весь список курсов"
  []
  (db/select 'Courses))

(defn get-course-information
  "Получить запись курса"
  [course-id]
  (db/select-one 'Courses :id course-id))

(defn build-tree
  "Построение дерева курса в виде вложенной хэш таблицы"
  [head elements]
  (let [head-id (:id head)
        leaf? (:leaf head)
        children (if leaf?
                   []
                   (get elements head-id))
        enriched-children (map #(build-tree % elements) children)]
    (assoc head :children enriched-children)))

(defn build-course-structure
  "Построение дерева курса. На выходе вложенная мапа курса с
  детьми"
  [course-id]
  (let [elements (get-by [TCourseElements :id :parent_id :display_name]
                         {:course_id course-id}
                         :order-by [:sort])
        documents (get-by [TDocuments :id :course_element_id :display_name :type]
                          {:course_id course-id}
                          :order-by [:sort])
        documents (map #(assoc % :parent_id (:course_element_id %)) documents)
        documents (map #(assoc % :parent_id (:course_element_id %) :leaf true) documents)
        elements (concat elements documents)
        grouped (group-by :parent_id elements)
        tree (build-tree (first (get grouped nil)) grouped)]
    tree))

(defn render-course-structure
  "Отрисовка структуры курса в текстовом формате"
  ([root-] (render-course-structure root- ""))
  ([root- ident]
   (let [icon (if (some? (:type root-))
                (get icons (:type root-))
                (get icons "folder"))
         new-line [(str ident icon (:display_name root-))]
         ident (if (s/blank? ident) "└ " (str "     " ident))
         new-lines (map #(render-course-structure % ident) (:children root-))]
     (flatten (concat new-line new-lines)))))
