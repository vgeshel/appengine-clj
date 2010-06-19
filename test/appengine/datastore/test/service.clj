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

(datastore-test test-delete-with-entity
  (is (delete (make-example-entity)))
  (let [entity (put (make-example-entity))]
    (is (= (get entity) entity))
    (is (delete entity))
    (is (nil? (get entity)))))

(datastore-test test-delete-with-key
  (let [key (make-example-key) entity (put key)]
    (is (= (get key) entity))
    (is (delete key))
    (is (nil? (get key)))
    (is (delete key))))

(datastore-test test-get-with-entity
  (is (nil? (get (make-example-entity))))    
  (let [entity (put (make-example-entity))]
    (is (= (get entity) entity))))

(datastore-test test-get-with-key
  (let [key (make-example-key)]
    (is (nil? (get key)))
    (let [entity (put key)]
      (is (= (get key) entity)))))

(datastore-test test-put-with-entity
  (let [entity (put (make-example-entity))]
    (is (entity? entity))
    (is (= (.getKey entity) (.getKey (make-example-entity))))
    (is (= (get entity) entity))
    (is (= (put entity) entity))))

(datastore-test test-put-with-key
  (let [key (make-example-key) entity (put key)]
    (is (entity? entity))
    (is (= (.getKey entity) key))
    (is (= (get key) entity))
    (is (= (put (.getKey entity)) entity))))

;; (datastore-test test-prepare
;;   (let [query (prepare (Query. "continent"))]
;;     (is (isa? (class query) PreparedQueryImpl))))
