(ns new-todo-bot.db.helpers.common
  (:require [new-todo-bot.db.conn :refer [db]]))

(defn transform-table-fields
  "Преобразование параметра table-fields в мапу table и fields отдельно"
  [table-fields]
  (cond
    (vector? table-fields) {:table (first table-fields)
                            :fields (subvec table-fields 1)}
    (keyword? table-fields) {:table table-fields
                             :fields [:*]}))

(defn get-by
  "Общая фунция для получения таблиц из БД"
  ([table-fields] (get-by table-fields {}))
  ([table-fields condition]
   (let [t&f (transform-table-fields table-fields)
         where-cond (map #(into [:=] %1) condition)
         where (if (<= (count condition) 1)
                 where-cond
                 (into [:and] where-cond))]
     (db :execute!
         {:select (:fields t&f)
          :from (:table t&f)
          :where where}
         {:return-keys true}))))

(defn build-where-condition
  [condition]
  (let [where-cond (map #(into [:=] %1) condition)
        where (if (<= (count condition) 1)
                where-cond
                (into [:and] where-cond))]
    where))
(comment
  (build-where-condition {:id 1})
  )
(defn update-by!
  [table-fields condition object]
  (println "condition" condition)
  (let [multi? (if (vector? object) true false)
        t&f (transform-table-fields table-fields)
        where (build-where-condition condition)
        sql-map {:update (:table t&f)
                 :set object
                 :where where
                 :returning (:fields t&f)}]
    (println sql-map)
    (db :execute! sql-map {:multi-rs multi?})))

(defn insert-into!
  "Общая функция для вставки записи в таблицу БД"
  [table-fields object]
  (let [multi? (if (vector? object) true false)
        t&f (transform-table-fields table-fields)
        object (cond
              (map? object) [object]
              (vector? object) object)
        sql-map {:insert-into (:table t&f)
                 :columns (keys (first object))
                 :values (map #(vals %1) object)
                 :returning (:fields t&f)}]
    (db :execute! sql-map {:multi-rs multi?})))

