(ns new-todo-bot.config
  (:require [environ.core :refer [env]]))

(def token (env :telegram-token))
(def gpt-token (env :gpt-token))
