(ns new-todo-bot.courses.keyboards
  (:require [clojure.set :refer [rename-keys]]
            [new-todo-bot.db.helpers.common :refer [get-page-data new-pager]]
            [new-todo-bot.db.helpers.constants :as const]
            [new-todo-bot.db.helpers.course-elements :refer [get-course-content]]
            [new-todo-bot.telegram.keyboards :as kb]
            [ring.util.codec :refer [form-encode]]))

(defn encode-callback-payload
  [url payload]
  (form-encode (into {:url url} payload)))


(defn get-icon
  [el]
  (get const/icons (get el :type const/folder-type)))

(defn new-node-button
  "Returns blah-blah-blah.

  1. `el` more about element
  2. `url` callback URL, more about this
  3. `payload` what is payload, what fields can it contains
  4. `opts`
      + `:get-id` the function to get ID from `el`, default `:id`.
      + `:get-name` the function to get name of `el`, default `:display_name`.
  "
  ;{:arglists '([el url]
  ;             [el url {:keys [...] :as payload}]
  ;             [el url {:keys [...] :as payload} {:keys [get-id get-name]}])}

  ([el callback-url & {:keys [get-id get-name payload]
                       :or   {get-id :id get-name :display_name payload {}}}]
   (kb/new-button
     (str (get-icon el) (get-name el))
     (encode-callback-payload callback-url (into {:id (get-id el)} payload)))))

(defn build-node-buttons
  ([elements callback-url] (build-node-buttons elements callback-url {}))
  ([elements callback-url opts]
   (letfn [(inner-new-node-button [el] (new-node-button el callback-url :payload opts))]
     (let [buttons (->> elements
                        (map inner-new-node-button)
                        (map kb/new-line)
                        (vec))]
       buttons))))

(defn new-dir-up-button
  [parent-id]
  (let [button (when parent-id (kb/new-button "⬆️", (encode-callback-payload "get_dir_above" {:id parent-id})))]
    button))

(defn new-pagination-button
  [page-number direction-icon parent-id course-id]
  (when (and page-number (or parent-id course-id))
    (let [type- (if parent-id const/element-type const/course-type)
          id (or parent-id course-id)]
      (kb/new-button
          direction-icon
          (encode-callback-payload "get_item" {:id id :page-number page-number :type type-})))))

(defn new-download-all-files-button
  [parent-id course-id]
  (when (or parent-id course-id)
    (let [callback (encode-callback-payload "get_course_files" {:parent-id parent-id :course-id course-id})]
      (kb/new-button "⬇️ Получить все файлы текущей директории", callback))))

(defn build-course-kb
  [& {:keys [course-id parent-id page-number page-size get-content] :as args
      :or   {page-number 1 page-size const/default-page-size get-content get-course-content}}]
  (let [get-content-partial (fn [& {:keys [return-count?] :as params}]
                              (let [prefixed-args (select-keys args [:course-id :parent-id])
                                    all-params (merge prefixed-args params)
                                    params-as-vector (into [] cat all-params)]
                                (apply get-content params-as-vector)))
        pager (new-pager page-number page-size get-content-partial)
        elements (get-page-data pager)
        get-type #(rename-keys (select-keys % [:entity]) {:entity :type})
        build-elements-buttons #(kb/new-line (new-node-button % "get_item" :payload (get-type %)))
        elements-buttons (map build-elements-buttons elements)
        documents (some #{const/document-type} (map :entity elements))
        navigation-buttons (kb/new-line (new-pagination-button (.get-prev-page-number pager) "⬅️" parent-id course-id)
                                        (new-dir-up-button parent-id)
                                        (new-pagination-button (.get-next-page-number pager) "➡️" parent-id course-id))
        download-all-files-button (when (some? documents)
                                    (-> (new-download-all-files-button parent-id course-id)
                                        (kb/new-line)))
        lines (concat
                elements-buttons
                (when navigation-buttons [navigation-buttons])
                (when download-all-files-button [download-all-files-button]))]
    lines))
