(ns new-todo-bot.telegram.keyboards)

(defn new-button
  ([text] (new-button text nil))
  ([text callback]
   (if (nil? callback)
     {:text text}
     {:text text :callback_data callback})))

(defn new-line
  [& buttons]
  (vec (remove nil? buttons)))

(defn new
  [& lines]
  (vec lines))

(defn new-reply-markup
  [& lines]
  (vec lines))

(defn single-button-kb
  [text callback]
  [(new-line (new-button text callback))])

