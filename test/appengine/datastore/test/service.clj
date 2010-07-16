(ns appengine.datastore.test.service
  (:refer-clojure :exclude (get))
  (:import (com.google.appengine.api.datastore
            DatastoreService DatastoreServiceConfig$Builder
            DatastoreServiceFactory DatastoreServiceImpl Entity
            Query PreparedQuery Transaction))
  (:use clojure.test appengine.datastore.entities appengine.datastore.keys
        appengine.datastore.service appengine.test))

(refer-private 'appengine.datastore.service)

(defn make-example-key []
  (make-key "continent" "eu"))

(defn make-example-entity[]
  (Entity. (make-example-key)))

(defn transaction?
  "Returns true if arg is a transaction, else false."
  [arg] (isa? (class arg) Transaction))

(datastore-test test-begin-transaction  
  (let [transaction (begin-transaction)]
    (is (transaction? transaction))
    (is (= transaction (current-transaction)))
    (commit-transaction transaction)))

(datastore-test test-active-transactions
  (begin-transaction)
  (let [transactions (active-transactions)]
    (is (every? transaction? transactions))
    (commit-transaction (current-transaction))))

(datastore-test test-current-transaction
  (is (nil? (current-transaction)))  
  (begin-transaction)
  (is (transaction? (current-transaction)))
  (commit-transaction (current-transaction))) 

(deftest test-datastore  
  (is (datastore? (datastore)))
  (is (datastore? (datastore (DatastoreServiceConfig$Builder/withDefaults)))))

(deftest test-datastore?
  (is (not (datastore? nil)))
  (is (not (datastore? "")))
  (is (datastore? (datastore))))

(datastore-test test-delete-entity
  (is (nil? (delete-entity nil)))
  (let [entity (put-entity (make-example-entity)) key (.getKey entity)]
    (is (= (get-entity key) entity))
    (is (delete-entity key))
    (is (nil? (get-entity key)))
    (is (delete-entity key))))

(datastore-test test-get-entity
  (is (nil? (get-entity nil)))
  (let [entity (make-example-entity) key (.getKey entity)]
    (is (nil? (get-entity key)))
    (let [entity (put-entity entity)]
      (is (= (get-entity key) entity)))))

(datastore-test test-put-entity
  (is (nil? (put-entity nil)))
  (let [entity (put-entity (make-example-entity)) key (.getKey entity)]
    (is (entity? entity))
    (is (= key (.getKey (make-example-entity))))
    (is (= (get-entity key) entity))
    (is (= (put-entity entity) entity))))

(datastore-test test-prepare
  (let [query (prepare-query (Query. "continent"))]
    (is (isa? (class query) PreparedQuery))))
