(ns new-todo-bot.courses.controllers
  (:require [clojure.string :as s]
            [new-todo-bot.common.utils :as u]
            [new-todo-bot.db.helpers.common :refer [get-by]]
            [new-todo-bot.db.helpers.constants :as const]
            [new-todo-bot.db.helpers.course-elements :refer [TCourseElements]]
            [new-todo-bot.db.helpers.courses :refer [Courses]]
            [new-todo-bot.telegram.senders :as ts]
            [morse.api :as t]
            [new-todo-bot.db.helpers.documents :refer [TDocuments]]
            [toucan.db :as db]
            [new-todo-bot.courses.keyboards :refer [build-course-kb]]))

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
        tree (map (fn [el] (build-tree el grouped)) (get grouped nil))]
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

(defn get-item-
  [token chat-id element-id type- & {:keys [page-number page-size]}]
  (let [send-keyboard (partial ts/send-keyboard token chat-id "Список курсов")
        element-id (u/parse-int element-id)
        build-kb (partial build-course-kb :page-number (u/parse-int page-number) :page-size (u/parse-int page-size))]
    (condp = type-
      const/document-type (->> (get-by TDocuments {:id element-id})
                               first
                               :tg_file_id
                               (t/send-document
                                 token
                                 chat-id))
      const/element-type (send-keyboard (build-kb :parent-id element-id))
      const/course-type (send-keyboard (build-kb :course-id element-id :parent-id nil))
      (t/send-text token chat-id (str "Не найден тип документа " type-)))))
