(ns new-todo-bot.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [morse.handlers :as h]
            [morse.polling :as p]
            [new-todo-bot.courses.views :as views])
  (:gen-class))

(def token "")

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
      :start_course views/start-course)))


(defn -main
  [& _]
  (when (str/blank? token)
    (println "Please provide token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the new-todo-bot")
  ;(def channel (p/start token handler))

  (<!! (p/start token handler))
  )
