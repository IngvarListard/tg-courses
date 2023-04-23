(ns cli.set-document-type
  (:require [new-todo-bot.db.helpers.common :refer [get-by update-by!]]
            [new-todo-bot.db.helpers.documents :refer [TDocuments document-types]]
            [new-todo-bot.db.helpers.course-elements :refer [TCourseElements]]
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

  (set-document-type {:display_name "asdfasdfasdf.mp3"}))

(defn set-display-name
  [element]
  (let [display-name (-> (:display_name element)
                         (s/replace #"_" " "))]
    (assoc element :display_name display-name)))

(defn set-sort
  "Проставить сортировку для курса A.J. Hoge на основе номеров директорий"
  [{:keys [display_name] :as element}]
  (let [sort-string (first (s/split display_name #" "))
        sort-position (try (Float/parseFloat sort-string)
                           (catch Exception _ nil))]
    (assoc element :sort sort-position)))
(comment
  (try (/ 1 1)
       (catch Exception ex (println ex)))
  )

(defn get-documents
  [course-id]
  (get-by [TDocuments :id :display_name :type] {:course-id course-id}))

(defn get-elements
  [course-id]
  (get-by [TCourseElements :id :display_name] {:course-id course-id}))

(defn update-document!
  [{:keys [id] :as document}]
  (update-by! TDocuments {:id id} (dissoc document :id)))

(defn update-element!
  [{:keys [id] :as element}]
  (update-by! TCourseElements {:id id} (dissoc element :id)))

(defn -main
  [& args]
  (db/transaction
    ;(doseq [document (get-documents 1)]
    ;  (->> document
    ;    (set-document-type)
    ;    (set-display-name)
    ;    (update-document!)))
    (doseq [element (get-elements 1)]
      (->> element
           (set-sort)
           (update-element!)))))
