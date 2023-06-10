(ns new-todo-bot.telegram.utils
  (:require [clojure.string :as s]
            [morse.api :as t]
            [new-todo-bot.config :refer [token]]))

(def ^:const escape-chars [#"\_" #"\*" #"\[" #"\]" #"\(" #"\)" #"\~" #"\`" #"\>" #"\#" #"\+" #"\-" #"\=" #"\|"
                           #"\{" #"\}" #"\." #"\!"])

(defn escape-md2
  [content]
  (reduce (fn [left right]
            (s/replace left (re-pattern right) (str "\\\\" right))) content escape-chars))


(comment
  (escape-md2 "_*[]")
  (println (escape-md2 "_1 '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'"))
  (t/send-text token 37521589 {:parse_mode "MarkdownV2"} "*Углубленное программирование на C/С++* - #курс *от Mailru* *Group*\n\nВ курсе идет ознакомление с инструментами и практиками использующимся в современной разработке, получение навыков написания корректного и гибкого кода на С++. Вы получите практические навыки и умения, необходимые специалистам по разработке программного обеспечения для участия в проектах промышленной разработки на языках C++.\n\n*Ссылка на курс:* [https://www.youtube.com/playlist?list=PLrCZzMib1e9qjGLjg83bCksf3N7FIy7jg](https://www.youtube.com/playlist?list=PLrCZzMib1e9qjGLjg83bCksf3N7FIy7jg)\n")
  (t/send-text token 37521589 {:parse_mode "MarkdownV2"} "*Углубленное программирование на C/С\\+\\+*")
  (t/send-text token 37521589 {:parse_mode "markdown"} "")
  (try
    (t/send-text token 37521589 {:parse_mode "MarkdownV2"} "\\#курс\nШкола мобильного дизайна от Яндекса\n\nЧто узнаем: об особенностях дизайна мобильных продуктов, прототипировании, анимации и принципах командной работы.\n\nДля кого: разработчики, дизайнеры интерфейсов\nДата: в любое время. Уроки публикуются раз в неделю.\n\nСмотреть: [https://www.youtube.com/playlist?list=PLLkvpHo_HuBPmL0SFkxBAEaV7pvL9mMth](https://www.youtube.com/playlist?list=PLLkvpHo_HuBPmL0SFkxBAEaV7pvL9mMth)")
    (catch Exception e
      (println e)
      ))
  )