(ns new-todo-bot.db.helpers.common
  (:require [new-todo-bot.db.conn :refer [db]]
            [new-todo-bot.db.helpers.constants :as const])
  (:import (clojure.lang IFn)))

(defn transform-table-fields
  "Преобразование параметра table-fields в мапу table и fields отдельно"
  [table-fields]
  (cond
    (vector? table-fields) {:table  (first table-fields)
                            :fields (subvec table-fields 1)}
    (keyword? table-fields) {:table  table-fields
                             :fields [:*]}))

(defn build-where-cond [condition]
  (let [where-cond (map #(into [:=] %1) condition)
        where (if (<= (count condition) 1)
                where-cond
                (into [:and] where-cond))]
    where))

(defn get-by
  "Общая фунция для получения таблиц из БД"
  ([table-fields condition & {:keys [order-by offset limit join]}]
   (let [t&f (transform-table-fields table-fields)
         where (build-where-cond condition)
         sql-map (merge {:select (:fields t&f)
                         :from   (:table t&f)
                         :where  where}
                        (when order-by {:order-by order-by})
                        (when offset {:offset offset})
                        (when limit {:limit limit})
                        (when join {:join order-by}))]
     (db :execute! sql-map {:return-keys true}))))

(defn build-where-condition
  [condition]
  (let [where-cond (map #(into [:=] %1) condition)
        where (if (<= (count condition) 1)
                where-cond
                (into [:and] where-cond))]
    where))

(defn update-by!
  [table-fields condition object]
  (println "condition" condition)
  (let [multi? (if (vector? object) true false)
        t&f (transform-table-fields table-fields)
        where (build-where-condition condition)
        sql-map {:update    (:table t&f)
                 :set       object
                 :where     where
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
                 :columns     (keys (first object))
                 :values      (map #(vals %1) object)
                 :returning   (:fields t&f)}]
    (db :execute! sql-map {:multi-rs multi?})))

(defprotocol PagerProtocol
  (get-next-page [p] "Returns Pager object of next page")
  (get-prev-page [p] "Returns Pager object of previews page")
  (get-page-data [p] "Returns data of current page")
  (get-total-count [p])
  (get-next-page-number [p])
  (get-prev-page-number [p])
  (get-last-page-number [p]))

(defrecord Pager
  [^Integer page-number ^Integer page-size ^IFn get-data-func ^Integer total-count]

  PagerProtocol

  (get-next-page [page] (update page :page-number inc))

  (get-prev-page
    [page]
    (if (= page-number 1)
      page
      (update page :page-number dec)))

  (get-page-data
    [_]
    (let [_ (println "get-page-data" " page-number " page-number " page-size " page-size)
          offset (* (dec page-number) page-size)]
      (get-data-func :offset offset :limit page-size)))

  (get-total-count [_] total-count)

  (get-next-page-number
    [_]
    (println "total count " total-count)
    (let [last-page-number (quot total-count const/default-page-size)
          next-page (inc page-number)]
      (if (> next-page last-page-number)
        nil
        next-page)))

  (get-prev-page-number
    [_]
    (if (<= page-number 1)
      nil
      (dec page-number))))

(defn new-pager
  [page-number page-size get-data-func]
  (let [total-count (:count (get-data-func :return-count? true))
        page-number (or page-number 1)
        page-size (or page-size const/default-page-size)]
    (->Pager page-number page-size get-data-func total-count)))

(defn apply-map
  [f & {:keys [] :as kwargs}]
  (let [vec-params (into [] cat kwargs)]
    (apply f vec-params)))

(defn get-one-by
  [table-fields condition & {:keys [order-by offset limit join] :as kwargs}]
  (first (apply-map (partial get-by table-fields condition) kwargs)))

