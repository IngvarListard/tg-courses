(defproject new-todo-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [environ "1.1.0"]
                 [morse "0.2.4"]
                 [migratus "1.4.9"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [com.mchange/c3p0 "0.9.5.5"]
                 [org.xerial/sqlite-jdbc "3.39.3.0"]
                 [com.github.seancorfield/next.jdbc "1.3.865"]
                 [com.layerware/hugsql "0.5.3"]
                 [org.clojure/core.async "1.6.673"]
                 [com.github.seancorfield/honeysql "2.4.1011"]
                 [toucan "1.18.0"]
                 [ring/ring-core "1.10.0"]
                 ]

  :plugins [[lein-environ "1.1.0"]
            [migratus-lein "0.7.3"]]

  :migratus {:store         :database
             :migration-dir "migrations"
             :db            {:dbtype "sqlite"
                             :dbname "resources/db/db.sqlite3"}}

  :main ^:skip-aot new-todo-bot.core
  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
