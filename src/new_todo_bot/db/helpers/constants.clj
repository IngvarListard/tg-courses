(ns new-todo-bot.db.helpers.constants
  (:require [new-todo-bot.db.helpers.documents :refer [document-types]]))

(def ^:const document-type "documents")
(def ^:const element-type "course_elements")
(def ^:const course-type "courses")
(def ^:const external-video-type "external-video")
(def ^:const element-types #{document-type element-type})
(def ^:const folder-type "folder")
(def ^:const icons
  {(:file document-types) "\uD83D\uDCC4 "
   folder-type "\uD83D\uDCC1 "
   (:audio document-types) "\uD83D\uDD0A "})

(def ^:const default-page-size 10)