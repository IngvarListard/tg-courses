(ns new-todo-bot.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [morse.api :as t]
            [new-todo-bot.tg-api.core :as kb]
            [new-todo-bot.tg-api.senders :as s])
  (:gen-class))

; TODO: fill correct token
(def token "599216546:AAHNU_hhusgOoKS0aEn_ocj1ukTOHzRrul4")


(h/defhandler handler

  (h/command-fn "start"
    (fn [{{id :id :as chat} :chat}]
      (println "Bot joined new chat: " chat)
      (t/send-text token id "Welcome to new-todo-bot!")))

  (h/command-fn "help"
    (fn [{{id :id :as chat} :chat}]
      (println "Help was requested in " chat)
      (t/send-text token id "Help is on the way")))


  (h/command-fn "list"
    (fn [all]
      (println "here is all \n" all)
      (def obj all)
      ;; (t/send-text token (:id all) "Help is on the way")
      ))

  (h/command-fn "todo"
    (fn [all]
      (println "here is all \n" all)
      (def obj all)
      ;; (t/send-text token (:id all) "Help is on the way")
      ))

  (h/message-fn
    (fn [{{id :id} :chat :as message}]
      (println "Intercepted message: " message)
      (s/send-keyboard token id "I don't do a whole lot ... yet.")))
  (kb/new-keyboard)

  )


(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the new-todo-bot")
  (def channel (p/start token handler))
  ;; (<!! (p/start token handler))
  )
