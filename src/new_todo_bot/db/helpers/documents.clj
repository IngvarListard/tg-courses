(ns new-todo-bot.db.helpers.documents
  (:require [toucan.models :refer [defmodel, IModel]]))

(defmodel Documents :documents IModel)

(defonce TDocuments :documents)