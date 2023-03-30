-- :name add-todo :! :n
-- :doc Insert todo
insert into todos (description, status)
values (:description, :status)


-- A ":result" value of ":1" specifies a single record
-- (as a hashmap) will be returned
-- :name todo-by-id :? :1
-- :doc Get character by id
select * from todos
where id = :id


-- Let's specify some columns with the
-- identifier list parameter type :i* and
-- use a value list parameter type :v* for IN()
-- :name todos-by-ids-specify-cols :? :*
-- :doc Characters with returned columns specified
select :i*:cols from todos
where id in (:v*:ids)
