(ns cli.load-course
  (:require [clojure.java.io :as io]
            [new-todo-bot.db.models.course-elements :refer [CourseElements]]
            [new-todo-bot.db.models.courses :refer [Courses]]
            [new-todo-bot.db.models.documents :refer [Documents]]
            [toucan.db :as db]
            [morse.api :as api]
            [new-todo-bot.core :refer [token]]
            [clojure.string :refer [starts-with?]]))

(defn list-directory [path]
  (map str (file-seq (io/file path))))

(def resources-dir (str (System/getProperty "user.dir") "/resources/courses"))

(def resources (list-directory resources-dir))

(defn join-path [base to]
  "Объединение путей независимо от ОС"
  (-> (io/file base)
      (.toPath)
      (.resolve to)
      (.toString)))

(defn insert-element!
  "Создание элемента"
  [name course-id parent-id]
  (:id (db/insert! 'CourseElements {:course_id    course-id
                                    :parent_id    parent-id
                                    :name         name
                                    :display_name name})))

(defn insert-document!
  "Создание документа"
  [name course-id element-id tg-file-id]
  (:id (db/insert! 'Documents {:course_id         course-id
                               :tg_file_id        tg-file-id
                               :course_element_id element-id
                               :name              name
                               :display_name      name})))

(defn insert-course!
  "Создание курса"
  [name]
  (:id (db/insert! 'Courses {:name         name
                             :display_name name})))

(defn send-file [file chat-id]
  (api/send-document token chat-id file))

(defn load-course
  ([path course-id] (load-course path course-id nil))
  ([path course-id parent-id]
   (println "parent-id is " parent-id)
   (let [file (io/file path)
         file-name (.getName file)]
     (if (.isDirectory file)
       (let [element-id (insert-element! file-name course-id parent-id)
             files (filter #(not (starts-with? %1 ".")) (.list file))]
         (doseq [sub-file files]
           (load-course (join-path path (str sub-file)) course-id element-id)))
       (let [resp (send-file file 37521589)
             tg-file-id (get-in resp [:result :document :file_id])]
         (insert-document! file-name course-id parent-id tg-file-id))))))


(defn -main
  [& args]
  (let [course-id (insert-course! "fist test course")]
    (db/transaction
      (load-course resources-dir course-id))))

(comment

  (println resources)
  (def f (io/file "/Users/igorlisovcov/Downloads"))
  (map #(join-path "/" (str %1)) (.list f))
  )
