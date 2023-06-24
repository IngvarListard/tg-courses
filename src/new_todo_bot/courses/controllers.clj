(ns new-todo-bot.courses.controllers
  (:require [clojure.string :as s]
            [new-todo-bot.common.utils :as u]
            [new-todo-bot.db.helpers.common :refer [get-by get-one-by apply-map]]
            [new-todo-bot.db.helpers.constants :as const]
            [new-todo-bot.db.helpers.course-elements :refer [TCourseElements]]
            [new-todo-bot.db.helpers.courses :refer [Courses TCourses]]
            [new-todo-bot.telegram.senders :as ts]
            [morse.api :as t]
            [new-todo-bot.db.helpers.documents :refer [TDocuments]]
            [toucan.db :as db]
            [new-todo-bot.courses.keyboards :refer [build-course-kb]]
            [new-todo-bot.db.helpers.user-last-course :refer [update-user-last-course!]]
            [new-todo-bot.db.helpers.users :refer [TUser]]
            [new-todo-bot.telegram.utils :as tu]
            [new-todo-bot.telegram.plain-to-html :as pth]
            [markdown.core :as md]))

(def ^:const icons
  {"file"   "\uD83D\uDCC4 "
   "folder" "\uD83D\uDCC1 "
   "audio"  "\uD83D\uDD0A "})

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
  [head elements & {:keys [depth current-depth]
                    :or   {current-depth 1 depth 2}}]
  (let [head-id (:id head)
        leaf? (:leaf head)
        children (if leaf?
                   []
                   (get elements head-id))
        current-depth* (inc current-depth)
        do-enrich-children (fn [els] (map #(build-tree % elements :depth depth :current-depth current-depth*) els))]
    (if (and depth (> current-depth* depth))
      head
      (assoc head :children (do-enrich-children children)))))

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
     (let [icon (get icons (:type root- "folder"))
           new-line [(str ident icon (:display_name root-))]
           ident (if (s/blank? ident) "└ " (str "     " ident))
           new-lines (map #(render-course-structure % ident) (:children root-))]
       (flatten (concat new-line new-lines)))))

(defmulti send-item
          (fn [typ token chat-id element-id send-keyboard] typ))

(defmethod send-item
  const/document-type
  [_ token chat-id element-id send-keyboard]
  (let [document (get-one-by TDocuments {:id element-id})
        {:keys [url display_name tg_file_id type description]} document]
    (if (= type const/external-video-type)
      (t/send-text token chat-id {:parse_mode "html"} (cond-> (u/md-link url display_name)
                                                                  description ((fn [l] (pth/md->html (str "\n" description l))))))
      (t/send-document token chat-id tg_file_id))))

(defmethod send-item
  const/element-type
  [_ token chat-id element-id send-keyboard]
  (let [kb-text "Список курсов: "
        text (->> (get-one-by [TCourseElements :display_name] {:id element-id})
                  :display_name
                  (str kb-text))]
    (send-keyboard token chat-id (pth/md->html text) :parent-id element-id)))

(defmethod send-item
  const/course-type
  [_ token chat-id element-id send-keyboard]
  (let [kb-text "Список курсов: "
        text (->> (get-one-by [TCourses :display_name] {:id element-id})
                  :display_name
                  (str kb-text))]
    (send-keyboard token chat-id (pth/md->html text) :course-id element-id :parent-id nil)))

(defn get-item-
  [token chat-id element-id type- & {:keys [page-number page-size send-keyboard]
                                     :or   {send-keyboard ts/send-keyboard}}]
  (let [element-id (u/parse-int element-id)
        send-keyboard (fn [token chat-id text & {:keys [] :as kwargs}]
                        (let [kb (apply-map build-course-kb (merge {:page-number (u/parse-int page-number)
                                                                    :page-size   (u/parse-int page-size)}
                                                                   kwargs))]
                          (send-keyboard token chat-id text kb)))]
    (send-item type- token chat-id element-id send-keyboard)))

(defn get&save-item-for-user
  [token chat-id element-id type- & {:keys [page-number page-size send-keyboard]
                                     :as   kwargs}]
  (let [user (get-by [TUser :id] {:telegram_id chat-id})
        user-id (-> user first :id)
        element-id* (u/parse-int element-id)]
    (update-user-last-course! user-id element-id* type-)
    (get-item- token chat-id element-id* type- kwargs)))
