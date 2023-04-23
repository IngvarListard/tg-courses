(ns cli.load-course
  (:require [clojure.java.io :as io]
            [new-todo-bot.db.helpers.course-elements :refer [CourseElements TCourseElements]]
            [new-todo-bot.db.helpers.courses :refer [Courses TCourses]]
            [new-todo-bot.db.helpers.documents :refer [Documents TDocuments]]
            [toucan.db :as db]
            [morse.api :as api]
            [new-todo-bot.core :refer [token]]
            [clojure.string :refer [starts-with?]]
            [new-todo-bot.db.helpers.common :refer [get-by]]
            [new-todo-bot.db.conn :refer [db]]))

(defn list-directory [path]
  (map str (file-seq (io/file path))))

(def resources-dir (str (System/getProperty "user.dir") "/resources/courses/Effortless English - New method learning english"))

(def resources (list-directory resources-dir))

(defn join-path
  "Объединение путей независимо от ОС"
  [base to]
  (-> (io/file base)
      (.toPath)
      (.resolve to)
      (.toString)))

(defn insert-element!
  "Создание элемента"
  [name course-id parent-id]
  (println "inserting " name course-id parent-id)
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

(defn ensure-course-exists!
  [name]
  (or
    (-> (get-by [TCourses :id] {:name name
                                :display_name name})
        first
        :id)
    (insert-course! name)))

(defn send-file [file chat-id]
  (api/send-document token chat-id file))

(comment

  (send-file
    (io/file
      "/Users/igorlisovcov/Documents/projects/new-todo-bot/resources/courses/Effortless English - New method learning english/Level 2/2.2_double_standard/Double Standard POV.mp3")
    37521589))

(defn update-document!
  [document-id file-name course-id parent-id tg-file-id]
  (db :execute!
      {:update TDocuments
       :set    {:course_id         course-id
                :tg_file_id        tg-file-id
                :course_element_id parent-id
                :name              file-name
                :display_name      file-name}
       :where  [:= :id document-id]}))

(defn ensure-document-exists!
  [file-name course-id parent-id tg-file-id]
  (if-let [document-id (-> (get-by [TDocuments :id]
                                   {:name file-name :course-id course-id :course_element_id parent-id})
                           first
                           :id)]
    (update-document! document-id file-name course-id parent-id tg-file-id)
    (insert-document! file-name course-id parent-id tg-file-id)))

(def to-create
  #{"Double Standard Audio.mp3"
    "Double Standard Commentary.mp3"
    "Greek Family POV.mp3"
    "Greek Family Audio.mp3"
    "Greek Family Vocab.mp3"
    "Obsessive Behavior Vocab.mp3"
    "Obsessive Behavior Audio.mp3"
    "Obsessive Behavior POV.mp3"
    "Longtime Affair POV.mp3"
    "Longtime Affair Audio.mp3"
    "Nudist Commentary.mp3"
    "Nudist Vocab.mp3"
    "Nudist POV-MS.mp3"
    "Nudist Audio.mp3"
    "Bad Choices Commentary.mp3"
    "Bad Choices POV.mp3"
    "Bad Choices Audio.mp3"
    "Lost Custody Vocab.mp3"
    "Lost Custody POV.mp3"
    "Lost Custody Audio.mp3"
    "Meddling Mother-In-Law.mp3"
    "Meddling Mother-In-Law POV.mp3"
    "EIQ Husbands MS-POV.mp3"
    "EIQ Husbands Article.mp3"
    "Our Universal Journey.mp3"
    "First Battle Audio.mp3"
    "First Battle POV.mp3"
    "First Battle Commentary.mp3"
    "Storytelling-Vocab.mp3"
    "Storytelling.mp3"
    "Disobedience.mp3"
    "Disobedience-Vocab.mp3"
    "Mind Maps Commentary.mp3"
    "Mind Maps.mp3"
    "Neo-Bedouins POV-MS.mp3"
    "Neo-Bedouins Audio.mp3"
    "Lifestyle Diseases.mp3"
    "New Year's Resolutions.mp3"
    "Cafe Puccini.mp3"
    "Cafe Puccini Vocab.mp3"
    "Validation Audio.mp3"
    "MS Censorship Commentary.mp3"
    "MS Censorship.mp3"
    "Thriving On Chaos.mp3"
    "Jack Kerouac.mp3"
    "Worthy Goals.mp3"
    "Worthy Goals MS.mp3"
    "TPR & Listen First.mp3"
    "TPR & Listen First MS-POV.mp3"
    "Role of Media 2 POV.mp3"
    "Role of Media 2 Audio.mp3"
    "Vipassana Commentary.mp3"
    "Vipassana.mp3"
    "Media 1 POV.mp3"
    "Media 1 Audio.mp3"
    "No Belief.mp3"
    "Hitch 3 Commentary.mp3"
    "Hitch 3 Audio.mp3"
    "Hitch 3 POV-MS.mp3"
    "Hitch 2.mp3"
    "Hitch 2 POV-MS.mp3"
    "Hitch Intro.mp3"
    "Hitch Intro MS-POV.mp3"
    "Hitch Intro Vocab.mp3"
    "Hitch 1.mp3"
    "Hitch 1 MS-POV.mp3"
    "Hitch 1 Vocab.mp3"
    "Bubba's Food Audio.mp3"
    "Intimacy Audio.mp3"
    "The Race Audio.mp3"
    "The Race POV.mp3"
    "Drag Audio.mp3"
    "Secret Love Audio.mp3"
    "A Kiss Vocab.mp3"
    "A Kiss Audio.mp3"
    "Changed Audio.mp3"
    "Day of the Dead Audio.mp3"
    "Day of the Dead Vocab.mp3"})

(defn load-course
  ([path course-id] (load-course path course-id nil))
  ([path course-id parent-id]
   (println "params " path course-id parent-id)
   (let [file (io/file path)
         file-name (.getName file)]
     (if (.isDirectory file)
       (let [element-id (or
                          (-> (get-by [TCourseElements :id] {:name      file-name
                                                             ;:course-id course-id
                                                             ;:parent-id parent-id
                                                             })
                              first
                              :id)
                          (insert-element! file-name course-id parent-id))
             files (filter #(not (starts-with? %1 ".")) (.list file))]
         (doseq [sub-file files]
           (load-course (join-path path (str sub-file)) course-id element-id)))
       (if-let [resp (if (contains? to-create file-name) (send-file file 37521589) nil)]
         (let [tg-file-id (or
                            (get-in resp [:result :document :file_id])
                            (get-in resp [:result :audio :file_id]))]
           (ensure-document-exists! file-name course-id parent-id tg-file-id))
         nil)))))

(defn -main
  [& args]
  (let [course-id (ensure-course-exists! "Effortless English - New method learning english")]
    (db/transaction
      (load-course resources-dir course-id))))

(comment
  (println resources)
  (def f (io/file "/Users/igorlisovcov/Downloads"))
  (map #(join-path "/" (str %1)) (.list f))
  )
