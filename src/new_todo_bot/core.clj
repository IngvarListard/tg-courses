(ns new-todo-bot.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [morse.handlers :as h]
            [morse.polling :as p]
            [new-todo-bot.courses.views :as views])
  (:gen-class))

(def token "6022297989:AAFsZ9UT34wg_8fcsIdAThxZ1aebvdHDRNQ")

;(defn command-fn [name handler]
;  (fn [update]
;    (let [update-command (-> update
;                             (get-in [:message :text])
;                             (str/split #"_")
;                             (first)
;                             (println)
;                             ((fn [cmd] (assoc-in update [:message :text] cmd))))]
;      (if (h/command? update-command name)
;        (handler (:message update-command))))))

(h/defhandler handler
  (h/command-fn "list" views/list-)
  (h/command-fn "start" views/start)
  (h/callback-fn
    (views/defcallback
      :get_course views/get-course
      :start_course views/start-course
      :get_item views/get-item)))


(defonce channel (p/start token handler))

(defn -main
  [& _]
  (when (str/blank? token)
    (println "Please provide token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the new-todo-bot")

  (<!! (p/start token handler))
  )

(defn restart-app
  []
  (p/stop channel)
  (def channel (p/start token handler)))


(comment
  (restart-app))