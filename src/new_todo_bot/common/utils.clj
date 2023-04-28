(ns new-todo-bot.common.utils)
(defn parse-int
  [s]
  (try
    (condp = (type s)
      String (Integer/parseInt s)
      Integer s
      Long (int s)
      Float (int s))
    (catch Exception _ nil)))
