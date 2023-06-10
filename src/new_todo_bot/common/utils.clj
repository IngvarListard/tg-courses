(ns new-todo-bot.common.utils
  (:require [clojure.string :as s]
            [new-todo-bot.telegram.utils :as tu]))
(defn parse-int
  [s]
  (try
    (condp = (type s)
      String (Integer/parseInt s)
      Integer s
      Long (int s)
      Float (int s))
    (catch Exception _ nil)))

(def filter-letters-only #"[^\p{L}\p{M}\p{N}\p{P}\p{Z}\p{Cf}\p{Cs}\s]")

(defn remove-spec-chars [s]
  (-> s
      (s/replace filter-letters-only "")
      (s/replace #"_" " ")))

(defn md-link
  ([url] (md-link url url))
  ([url text]
   (cond-> text
           text (tu/escape-md2)
           :always (#(format "[%s](%s)" % url)))))

(defn md-bold
  [text]
  (format "*%s*" (tu/escape-md2 text)))

(comment
  (md-link "http://asdfas.com")

  )