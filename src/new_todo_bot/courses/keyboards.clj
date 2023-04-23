(ns new-todo-bot.courses.keyboards
  (:require [new-todo-bot.telegram.keyboards :as kb]
            [ring.util.codec :refer [form-encode]]
            [new-todo-bot.db.helpers.course-elements :refer [get-course-elements]]
            [new-todo-bot.db.helpers.constants :as const]))

(defn build-node-buttons
  ([elements get-name get-id callback-url] (build-node-buttons elements get-name get-id callback-url {}))
  ([elements get-name get-id callback-url opts]
   (letfn [(get-icon [el] (get  const/icons (get el :type "folder")))]
     (vec (map #(kb/new-button
                  (str (get-icon %) (get-name %))
                  (form-encode (into {:url callback-url :id (get-id %)} opts)))
               elements)))))

(defn build-course-kb
  [& {:keys [course-id parent-id] :as args}]
  (let [[elements documents] (get-course-elements args)
        elements-buttons (build-node-buttons elements :display_name :id "get_item" {:type const/element-type})
        documents-buttons (build-node-buttons documents :display_name :id "get_item" {:type const/document-type})
        lines (map kb/new-line (concat elements-buttons documents-buttons))]
    lines))
