(ns new-todo-bot.common.middlewares
  (:require [morse.api :as t]
            [environ.core :refer [env]]
            [clojure.pprint :refer [pprint]]))

(def token (env :telegram-token))

(defn exception-middleware
  [func]
  (fn [message]
    (try
      (func message)
      (catch Exception e
        (let [chat-id (or (-> message :callback_query :message :chat :id)
                          (-> message :message :chat :id))]
          (println "Произошло исключение\n" e "\n" "Для сообщения: ")
          (pprint message)
          (t/send-text token chat-id "Упс! Что-то пошло не так"))))))
