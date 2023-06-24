(ns new-todo-bot.telegram.plain-to-html
  (:require [clojure.string]
            [markdown.common
             :refer
             [bold
              bold-italic
              dashes
              em
              escape-inhibit-separator
              escaped-chars
              heading-text
              inhibit
              inline-code
              italics
              strikethrough
              strong
              thaw-strings]]
            [markdown.core :as md]
            [markdown.links
             :refer [image
                     image-reference-link
                     implicit-reference-link
                     link
                     ]]
            [markdown.tables :refer [table]]
            [markdown.transformers :as ts]
            [morse.api :as t]
            [new-todo-bot.config :refer [token]]))

(defn heading [text {:keys [buf next-line code codeblock heading-anchors] :as state}]
  (cond
    (or codeblock code)
    [text state]

    (ts/h1? (or buf next-line))
    [(str "<b>" text "</b>" "\n") state]

    (ts/h2? (or buf next-line))
    [(str "<b>" text "</b>" "\n") state]

    :else
    (if-let [heading-text* (heading-text text)]
      [(str heading-text* "\n") state]
      [text state])))

(defn escape-tg-markdown
  "Change special characters into HTML character entities."
  [text state]
  [(if-not (or (:code state) (:codeblock state))
     ;(escape-md2 text)
     (clojure.string/escape
       text
       {\& "&amp;"
        \< "&lt;"
        \> "&gt;"
        \" "&quot;"})
     text) state])

(defn escape-tg-specials
  [text state]
  [(if-not (or (:code state) (:codeblock state))
     (clojure.string/escape
       text
       {\. "\\."
        \_ "\\_"})
     text) state])

(def transformers
  [escape-tg-markdown
   ts/set-line-state
   ts/empty-line
   inhibit
   escape-inhibit-separator
   ts/code
   ts/codeblock
   escaped-chars
   inline-code
   ts/autoemail-transformer
   ts/autourl-transformer
   image
   image-reference-link
   link
   implicit-reference-link
   ;reference-link
   ;footnote-link
   ts/hr
   ts/blockquote-1
   ;li
   heading
   ;ts/heading
   ts/blockquote-2
   italics
   bold-italic
   em
   strong
   bold
   strikethrough
   ts/superscript
   table
   ;paragraph
   ts/br
   thaw-strings
   dashes
   ts/clear-line-state])

(defn md->html
  [text]
  (md/md-to-html-string text :replacement-transformers transformers))

(comment
  (md/md-to-html-string "#foo" :replacement-transformers tts)
  (println (md/md-to-html-string "# This is a test\nsome code follows\n```clojure\n(defn foo [])\n```"))
  )

(comment
  (try
    (t/send-text token 37521589 {:parse_mode "HTML"}
                 (md/md-to-html-string "*Управление IT\\-проектами и продуктом* \\- \\#курс *от Mailru Group*\n\nВ курсе рассмотрены теория и практика по управлению продуктом и всем, что есть внутри \\(или рядом с ним\\): процессами, требованиями, метриками, сроками, запусками и, конечно — про людей и как с ними общаться\\.\n\n*Ссылка на курс:* [https://www\\.youtube\\.com/playlist?list\\=PLrCZzMib1e9oUFO9saNfPAqBjpQW8v9I\\-](https://www.youtube.com/playlist?list=PLrCZzMib1e9oUFO9saNfPAqBjpQW8v9I-)" :replacement-transformers tts))
    (catch Exception e
      (println e)
      ))
  (try
    (t/send-text token 37521589 {:parse_mode "HTML"}
                 "<br>asdf")
    (catch Exception e
      (println e)
      ))
  )