#!/usr/bin/env bb
(ns cli.load-from-tg-json)

;; Load HoneySQL from Clojars:
(deps/add-deps '{:deps {com.github.seancorfield/honeysql {:mvn/version "2.4.1011"}}})
(pods/load-pod 'org.babashka/postgresql "0.1.0")

(require '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[babashka.process :refer [shell process exec]]
         '[babashka.deps :as deps]
         '[babashka.pods :as pods]
         '[pod.babashka.postgresql :as pg]
         '[honey.sql :as hsql]
         '[clojure.string :as s])

(def db {:dbtype   "postgresql"
         :host     "localhost"
         :dbname   "courses"
         :user     "postgres"
         :password "postgres"
         :port     5432})

(pg/with-transaction
  (pg/execute! db (hsql/format {:select :* :from :courses})))

(comment
  (with-open [reader (io/reader "/Users/igorlisovcov/Documents/projects/babashka_test/src/result.json")]
    (let [f (slurp reader)]
      (def data (json/parse-string f true))
      (def messages (:messages data))
      (def mess (subvec messages 2 3))
      (def with-course-tags
        (filter (fn [message]
                  (let [text (:text message)]
                    (some true? (map (fn [t]
                                       (and
                                         (= (:text t) "#курс")
                                         (= (:type t) "hashtag"))) text))))
                messages))
      (def youtube-courses (filter (fn [message]
                                     (let [text (:text message)]
                                       (some true? (map (fn [t]
                                                          (and
                                                            (= (:type t) "link")
                                                            (str/includes? (:text t) "youtu")
                                                            (str/includes? (:text t) "playlist"))) text))))
                                   with-course-tags))
      (def courses (json/generate-string youtube-courses))
      (with-open [writer (io/writer "/Users/igorlisovcov/Documents/projects/babashka_test/src/youtube-courses-playlists.json")]
        (.write writer courses))
      )
    )
  )

(defn get-playlist-title
  [url]
  (-> (shell {:out :string} "yt-dlp" "--get-filename" "-o" "'%(playlist_title)s'" "--flat-playlist" "--playlist-end"
             "1" "--skip-download" url) :out s/trim (s/replace #"'" "")))

(defn md-link
  ([url] (md-link url url))
  ([url text]
   (format "[%s](%s)" text url)))

(defn md-bold
  [text]
  (format "*%s*" text))

(defn format-description
  [text]
  (reduce (fn [left right]
            (if (string? right)
              (str left right)
              (let [{:keys [type text]} right]
                (case type
                  "link" (str left (md-link text))
                  "bold" (str left (md-bold text))
                  (str left text)))))
          "" text))

(comment
  (format-description (first youtube-courses))
  )

(defn create-course-item
  [url title text]
  (first (pg/execute! db (hsql/format {:insert-into :courses
                                       :columns     [:name :display_name :source :source_url :description]
                                       :values      [[title title "youtube" url text]]
                                       :returning   [:*]}))))

(defn create-document-item
  [course-id display-name url description sort]
  (first (pg/execute! db (hsql/format {:insert-into :documents
                                       :columns     [:name :display_name :url :course_id :type :description :sort]
                                       :values      [[display-name display-name url course-id "external-video" description sort]]
                                       :returning   [:*]}))))

(defn get-playlist-url
  [text]
  (some (fn [t]
          (when (and
                  (= (:type t) "link")
                  (str/includes? (:text t) "youtu")
                  (str/includes? (:text t) "playlist"))
            (:text t))) text))

(defn create-course
  [{:keys [text]}]
  (let [url (get-playlist-url text)
        title (get-playlist-title url)
        desc (format-description text)]
    (create-course-item url title desc)))

(comment
  (create-course (first youtube-courses))
  )

(defn create-playlist-videos
  [url course-id]
  (let [proc (process {:err :inherit} "yt-dlp" "--get-id" "--get-title" url "-i")]
    (with-open [rdr (io/reader (:out proc))]
      (doseq [[idx [video-title video-id]] (map-indexed vector (partition 2 (line-seq rdr)))]
        (let [video-url (format "https://www.youtube.com/watch?v=%s" video-id)
              desc (->
                     (shell {:out :string} "yt-dlp" "--get-description" video-url "-i")
                     :out)]
          (prn (str idx " | " video-id " | " video-title))
          (create-document-item course-id video-title video-url desc (+ idx 1))
          )))))

(comment
  (create-playlist-videos "https://www.youtube.com/playlist?list=PLRDzFCPr95fK7tr47883DFUbm4GeOjjc0" 1)
  )

(defn -main
  []
  (pg/with-transaction
    (map (fn [course]
           (let [db-course (create-course course)]
             (prn "db-course" db-course)
             (create-playlist-videos (:courses/source_url db-course)
                                     (:courses/id db-course))))
         youtube-courses))
  )
