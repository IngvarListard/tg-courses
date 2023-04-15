(ns new-todo-bot.db.funcs.documents
  (:require [toucan.models :refer [defmodel, IModel]]))

(defmodel Documents :documents IModel)
