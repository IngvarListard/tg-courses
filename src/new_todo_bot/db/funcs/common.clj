(ns new-todo-bot.db.funcs.common
  (:require [new-todo-bot.db.conn :refer [db]]))

(defn transform-table-fields
  [table-fields]
  (cond
    (vector? table-fields) {:table (first table-fields)
                            :fields (subvec table-fields 1)}
    (keyword? table-fields) {:table table-fields
                             :fields [:*]}))

(defn get-by
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
          :where where}))))

(defn insert-into!
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
    (println  sql-map)
    (db :execute! sql-map {:multi-rs multi?})))

