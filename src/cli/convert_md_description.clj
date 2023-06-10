(ns cli.convert-md-description
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [new-todo-bot.common.utils :as u]
            [new-todo-bot.telegram.utils :as tu]
            [new-todo-bot.db.helpers.common :refer [update-by!]]
            [clojure.string :as s]
            [morse.api :as t]
            [new-todo-bot.config :refer [token]]))


(defn read-file
  [file-name]
  (let [file (io/resource file-name)]
    (slurp file)))

(defn format-description
  [text]
  (reduce (fn [left right]
            (if (string? right)
              (str left (tu/escape-md2 right))
              (let [{:keys [type text]} right]
                (case type
                  "link" (str left (u/md-link text))
                  "bold" (str left (u/md-bold text))
                  (str left (tu/escape-md2 text))))))
          "" text))

(defn get-playlist-url
  [text]
  (some (fn [t]
          (when (and
                  (= (:type t) "link")
                  (s/includes? (:text t) "youtu")
                  (s/includes? (:text t) "playlist"))
            (:text t))) text))

(defn update-description
  [src-url description]
  (update-by! :courses {:source_url src-url} {:description description}))

(comment
  (try
    (t/send-text token 37521589 {:parse_mode "MarkdownV2"} "\\#курс\nШкола мобильного дизайна от Яндекса\n\nЧто узнаем: об особенностях дизайна мобильных продуктов, прототипировании, анимации и принципах командной работы\\.\n\nДля кого: разработчики, дизайнеры интерфейсов\nДата: в любое время\\. Уроки публикуются раз в неделю\\.\n\nСмотреть: [https://www.youtube.com/playlist?list=PLLkvpHo_HuBPmL0SFkxBAEaV7pvL9mMth](https://www.youtube.com/playlist?list=PLLkvpHo_HuBPmL0SFkxBAEaV7pvL9mMth)")
    (catch Exception e
      (println e)
      ))
  )


(defn -main []
  (let [messages (-> "json/youtube-courses-playlists.json"
                     read-file
                     (json/parse-string true))]
    (doseq [m messages]
      (let [text (:text m)
            url (get-playlist-url text)
            desc (format-description text)]
        (update-description url desc)))))