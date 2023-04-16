(ns new-todo-bot.courses.keyboards
  (:require [new-todo-bot.telegram.keyboards :as kb]
            [ring.util.codec :refer [form-encode]]
            [new-todo-bot.db.helpers.course-elements :refer [get-course-elements]]
            [new-todo-bot.db.helpers.constants :as const]))

(defn build-node-buttons
  ([elements get-name get-id callback-url] (build-node-buttons elements get-name get-id callback-url {}))
  ([elements get-name get-id callback-url opts]
   (map #(kb/new-button
           (get-name %)
           (form-encode (into {:url callback-url :id (get-id %)} opts)))
        elements)))

(defn build-course-kb
  [& {:keys [course-id parent-id] :as args}]
  (let [[elements documents] (get-course-elements args)
        elements-buttons (build-node-buttons elements :name :id "get_item" {:type const/element-type})
        documents-buttons (build-node-buttons documents :name :id "get_item" {:type const/document-type})
        lines (map kb/new-line (into elements-buttons documents-buttons))]
    lines))
