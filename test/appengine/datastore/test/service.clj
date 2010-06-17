(ns appengine.datastore.test.service
  (:refer-clojure :exclude (get))
  (:import (com.google.appengine.api.datastore DatastoreService DatastoreServiceConfig$Builder
            DatastoreServiceFactory DatastoreServiceImpl Entity))
  (:use clojure.test appengine.datastore.entities appengine.datastore.keys
        appengine.datastore.service appengine.test))

(defn make-example-key []
  (make-key "continent" "eu"))

(defn make-example-entity[]
  (Entity. (make-example-key)))

(deftest test-current-transaction
  (current-transaction)) ;; TODO

(deftest test-datastore-service  
  (is (isa? (class (datastore-service)) DatastoreService))
  (is (isa? (class (datastore-service (DatastoreServiceConfig$Builder/withDefaults)))
            DatastoreService)))

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
