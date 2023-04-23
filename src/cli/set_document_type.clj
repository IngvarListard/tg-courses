(ns cli.set-document-type
  (:require [new-todo-bot.db.helpers.common :refer [get-by update-by!]]
            [new-todo-bot.db.helpers.documents :refer [TDocuments document-types]]
            [clojure.string :as s]
            [toucan.db :as db]))

(defn set-document-type
  [{:keys [display_name] :as document}]
  (let [matcher (re-matcher #"(?<type>.mp3|.txt|.pdf)" display_name)
        type- (first (re-find matcher))
        set-doc-type #(assoc % :type (:file document-types))]
    (condp = type-
      ".mp3" (assoc document :type (:audio document-types))
      ".pdf" (set-doc-type document)
      ".txt" (set-doc-type document)
      document)))

(comment
  (def matcher (re-matcher #"(?<type>.mp3|.txt|.pdf)" "asdfasdfads.mp3"))
  (re-groups matcher)

  (set-document-type {:display_name "asdfasdfasdf.mp3"})
  )

(defn set-display-name
  [element]
  (let [display-name (-> (:display_name element)
                         (s/replace #"_" " "))]
    (assoc element :display_name display-name)))

(comment
  (set-display-name {:display_name "adsf_adf_eqwr"})
  )

(defn get-documents
  [course-id]
  (get-by [TDocuments :id :display_name :type] {:course-id course-id}))

(defn update-document!
  [{:keys [id] :as document}]
  (update-by! TDocuments {:id id} (dissoc document :id)))

(defn -main
  [& args]
  (db/transaction
    (doseq [document (get-documents 1)]
      (->> document
        (set-document-type)
        (set-display-name)
        (update-document!)))))
