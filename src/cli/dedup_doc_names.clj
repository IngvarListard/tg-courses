(ns cli.dedup-doc-names
  (:require [clojure.string :as s]
            [new-todo-bot.db.helpers.common :refer [get-by update-by!]]
            [new-todo-bot.db.helpers.constants :as const]))


(def digits-filter #"\d+\.?")


(defn string-set [s]
  (reduce (fn [left right]
            (if (= (count left) 0)
              (let [r (s/trim (s/replace right digits-filter "\\\\d+\\.?"))]
                (if (not= (count r) 0)
                  (conj left r)
                  left))
              (conj left (s/trim (str (last left) " " (s/replace right digits-filter "\\\\d+\\.?"))))))
          []
          (s/split s #" ")))

(defn find-prefix [s1 s2]
  (let [s1-set (string-set s1)
        s2-set (string-set s2)]
    (->> (map-indexed (fn [idx item]
                        (when-let [item2 (get s2-set idx)]
                          (when (= item item2) item)))
                      s1-set)
         (remove nil?)
         last)))

(comment
  (find-prefix
    "1. Введение в Java. Вводное занятие.  Технострим"
    "2. Введение в Java. Тестирование.  Технострим")
  (find-prefix
    "ВЫУЧИМ 7000 СЛОВ - СУПЕР ТРЕНИРОВКА 2. АНГЛИЙСКИЙ ЯЗЫК  АНГЛИЙСКИЕ СЛОВА С ТРАНСКРИПЦИЕЙ И ПЕРЕВОДОМ"
    "ВЫУЧИМ 7000 СЛОВ - СУПЕР ТРЕНИРОВКА 3. АНГЛИЙСКИЙ ЯЗЫК  АНГЛИЙСКИЕ СЛОВА С ТРАНСКРИПЦИЕЙ И ПЕРЕВОДОМ")
  (find-prefix
    "1. Углублённое программирование на C/C. Введение  Технострим"
    "2. Углублённое программирование на C/С. Память в C  Технострим")
  (find-prefix
    "Photoshop - Лайфхаки и фишки  Урок 1"
    "Photoshop - Лайфхаки и фишки  Урок 2")
  (find-prefix
    "Курс Excel Базовый - Урок 2. Структура книги в Excel"
    "Курс Excel Базовый - Урок 3. Ячейка в Excel - основа всего!")
  (s/replace "001. Школа мобильного дизайна – Идея, исследование, концепт (Часть 1). Антон Тен" (re-pattern (find-prefix
                                                                                                              "001. Школа мобильного дизайна – Идея, исследование, концепт (Часть 1). Антон Тен"
                                                                                                              "011. Школа мобильного дизайна – Работа в команде. Юрий Подорожный"
                                                                                                              )) "")
  (string-set "1. Введение в Java. Вводное занятие.  Технострим")
  (s/replace "1. Введение в Java. Вводное занятие.  Технострим" digits-filter "")
  (apply sorted-set-by #(< (count %1) (count %2)) (string-set "ВЫУЧИМ 7000 СЛОВ - СУПЕР ТРЕНИРОВКА 2. АНГЛИЙСКИЙ ЯЗЫК  АНГЛИЙСКИЕ СЛОВА С ТРАНСКРИПЦИЕЙ И ПЕРЕВОДОМ"))

  (let [docs (get-by :documents {:type const/external-video-type} :order-by :sort)
        docs-grouped (group-by #(:course_id %) docs)]
    (println "docs grouped" docs-grouped)
    (doseq [course docs-grouped]
      (let [[_ documents] course
            prefix (or
                     (find-prefix (-> documents first :display_name) (-> documents last :display_name))
                     (find-prefix (-> documents second :display_name) (-> documents reverse second :display_name)))]
        (println "prefix: " prefix (first prefix))
        (when (seq prefix)
          (doseq [[i d] (map-indexed vector documents)]
            (try
              (let [display-name (s/trim (s/replace (:display_name d) (re-pattern prefix) ""))
                    display-name (if (<= (count display-name) 3) (str "Урок " (+ i 1)) display-name)]
                (update-by! :documents {:id (:id d)} (assoc d :display_name display-name)))
                (catch Exception e
                  ))))))
    )
  )