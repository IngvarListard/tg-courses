(ns cli.dedup-doc-names
  (:require [clojure.string :as s]
            [new-todo-bot.db.helpers.common :refer [get-by update-by!]]))


(def digits-filter #"\d+\.?")


(defn string-set [s]
  (reduce (fn [left right]
            (if (= (count left) 0)
              (let [r (s/trim (s/replace right digits-filter ""))]
                (if (not= (count r) 0)
                  (conj left r)
                  left))
              (conj left (s/trim (str (last left) " " (s/replace right digits-filter ""))))))
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
  (string-set "1. Введение в Java. Вводное занятие.  Технострим")
  (s/replace "1. Введение в Java. Вводное занятие.  Технострим" digits-filter "")
  (apply sorted-set-by #(< (count %1) (count %2)) (string-set "ВЫУЧИМ 7000 СЛОВ - СУПЕР ТРЕНИРОВКА 2. АНГЛИЙСКИЙ ЯЗЫК  АНГЛИЙСКИЕ СЛОВА С ТРАНСКРИПЦИЕЙ И ПЕРЕВОДОМ"))

  (let [docs (get-by :courses [])
        docs-grouped (group-by #(:course_id %) docs)]
    ;; TODO здесь надо найти префиксы для всех сгруппированных доксов. Если префикс есть - перезаписать у всех док имя
    )
  )