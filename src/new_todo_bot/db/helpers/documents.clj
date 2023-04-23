(ns new-todo-bot.db.helpers.documents
  (:require [toucan.models :refer [defmodel, IModel]]))

(defmodel Documents :documents IModel)

(def ^:const TDocuments :documents)

(def ^:const document-types {:file "file" :audio "audio"})

