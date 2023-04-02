(ns new-todo-bot.core
  (:require [clojure.string :as str]
            [morse.handlers :as h]
            [morse.polling :as p]
            [new-todo-bot.todos.controllers :as views])
  (:gen-class))

(def token "6022297989:AAFsZ9UT34wg_8fcsIdAThxZ1aebvdHDRNQ")

(h/defhandler handler
  (h/command-fn "start" views/start)
  (h/command-fn "help" views/help)
  (h/command-fn "list" views/list-)
  (h/command-fn "todo" views/todo)
  (h/message-fn views/default))


(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the new-todo-bot")
  (def channel (p/start token handler))

  ;(<!! (p/start token handler))
  )
