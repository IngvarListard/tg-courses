(ns new-todo-bot.courses.controllers
  (:require [new-todo-bot.db.helpers.course-elements :refer [CourseElements]]
            [new-todo-bot.db.helpers.courses :refer [Courses]]
            [new-todo-bot.db.helpers.documents :refer [Documents]]
            [toucan.db :as db]
            [clojure.string :as s]
            [new-todo-bot.db.conn :refer [db]]))

(def icons
  {"file" "\uD83D\uDCC4 "
   "folder" "\uD83D\uDCC1 "})

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
        enriched-children (map #(build-tree % elements) children)
        ]
    (assoc head :children enriched-children)
    ))

(comment
  (let [a {nil [{:id 1}]
           1 [{:id 2}
              {:id 3}]
           2 [{:id 4}
              {:id 5}]
           }]
    (build-tree (first (get grouped nil)) grouped))


  )

(defn build-course-structure
  "Построение дерева курса. На выходе вложенная мапа курса с
  детьми"
  [course-id]
  (let [elements (db/select
                   ['CourseElements :id :parent_id :display_name]
                   :course_id course-id)
        documents (db/select
                    ['Documents :id :course_element_id :display_name :type]
                    :course_id course-id)
        documents (map #(assoc % :parent_id (:course_element_id %)) documents)
        documents (map #(assoc % :parent_id (:course_element_id %) :leaf true) documents)
        elements (concat elements documents)
        grouped (group-by :parent_id elements)
        tree (build-tree (first (get grouped nil)) grouped)]
    tree))

(comment
  (def docs (db/select
              ['Documents :id :course_element_id :display_name :type]
              :course_id 1))
  (def els (db/select
             ['CourseElements :id :parent_id :display_name]
             :course_id 1))
  (def dd (map #(assoc % :parent_id (:course_element_id %)) docs))
  (def aa (concat dd els))

  (def grouped (group-by :parent_id aa))
  )

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
