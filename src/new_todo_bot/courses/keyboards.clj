(ns new-todo-bot.courses.keyboards
  (:require [new-todo-bot.db.helpers.constants :as const]
            [new-todo-bot.db.helpers.course-elements :refer [get-course-elements]]
            [new-todo-bot.telegram.keyboards :as kb]
            [ring.util.codec :refer [form-encode]]))

(defn encode-callback-payload
  [url payload]
  (form-encode (into {:url url} payload)))

(defn build-node-buttons
  ([elements get-name get-id callback-url] (build-node-buttons elements get-name get-id callback-url {}))
  ([elements get-name get-id callback-url opts]
   (letfn [(get-icon [el] (get const/icons (get el :type const/folder-type)))
           (new-node-button [el] (kb/new-button
                                   (str (get-icon el) (get-name el))
                                   (encode-callback-payload callback-url (into {:id (get-id el)} opts))))]
     (let [buttons (->> elements
                        (map new-node-button)
                        (map kb/new-line)
                        (vec))]
       buttons))))

(defn new-navigation-buttons
  [parent-id]
  (when parent-id
    (kb/new-line (kb/new-button "⬅️", (encode-callback-payload "get_dir_above" {:id parent-id}))
                 (kb/new-button "⬆️", (encode-callback-payload "get_dir_above" {:id parent-id}))
                 (kb/new-button "➡️", (encode-callback-payload "get_dir_above" {:id parent-id})))))

(defn new-download-all-files-button
  [parent-id course-id]
  (when parent-id
    (let [callback (encode-callback-payload "get_course_files" {:parent-id parent-id :course-id course-id})]
      (kb/new-line (kb/new-button "Получить все файлы курса", callback)))))

(defn build-course-kb
  [& {:keys [course-id parent-id] :as args}]
  (let [[elements documents] (get-course-elements args)
        elements-buttons (build-node-buttons elements :display_name :id "get_item" {:type const/element-type})
        documents-buttons (build-node-buttons documents :display_name :id "get_item" {:type const/document-type})
        navigation-buttons (new-navigation-buttons parent-id)
        download-all-files-button (when (not-empty documents) (new-download-all-files-button parent-id course-id))
        lines (concat
                elements-buttons
                documents-buttons
                (when navigation-buttons [navigation-buttons])
                (when download-all-files-button [download-all-files-button]))]
    lines))
(comment
  (build-course-kb {:course-id 1 :parent-id 10})
  )