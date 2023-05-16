(ns new-todo-bot.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [morse.handlers :as h]
            [morse.polling :as p]
            [new-todo-bot.common.middlewares :refer [exception-middleware]]
            [new-todo-bot.config :refer [token]]
            [new-todo-bot.courses.views :as views])
  (:gen-class))

(h/defhandler handler
  (h/command-fn "list" views/list-)
  (h/command-fn "start" views/start)
  (h/command-fn "last_course" views/get-last-requested-course)
  (h/command-fn "next_course" views/get-next-course)
  (h/command-fn "ask_mentor" views/ask-mentor)
  (h/callback-fn
    (views/defcallback
      :get_course views/get-course
      :start_course views/start-course
      :get_item views/get-item
      :get_dir_above views/get-dir-above
      :get_course_files views/get-course-files))
  (h/message message (println "Intercepted message:" message)))

(defonce channel (p/start token (exception-middleware handler)))

(defn -main
  [& _]

  (when (str/blank? token)
    (println "Please provide token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the new-todo-bot")

  (let [ch (p/start token handler)]
    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. ^Runnable (fn []
                           (println "Stopping gracefully")
                           (p/stop ch)
                           (System/exit 0))))
    (<!! ch)))

(defn restart-app
  []
  ;(p/stop channel)
  (def channel (p/start token (exception-middleware handler))))

(comment
  (restart-app))