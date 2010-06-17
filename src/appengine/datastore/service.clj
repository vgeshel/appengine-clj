(ns #^{:author "Roman Scherer"
       :doc "Low-level API for the Google App Engine datastore service."}
  appengine.datastore.service
  (:refer-clojure :exclude (get))
  (:import (com.google.appengine.api.datastore
            DatastoreServiceFactory DatastoreServiceConfig DatastoreServiceConfig$Builder
            Entity EntityNotFoundException Key))
  (:use [clojure.contrib.def :only (defvar)]))

(defprotocol Protocol
  (delete [entity] "Delete the entity, key or sequence of entities and
  keys from the datastore.")
  (get [entity] "Get the entity, key or sequence of entities and keys
  from the datastore.")
  (put [entity] "Put the entity, key or sequence of entities and keys
  to the datastore."))

(defn datastore
  "Returns a DatastoreService with the default or the provided
  configuration.

Examples:

  (datastore)
  ; => #<DatastoreServiceImpl DatastoreServiceImpl@a7b68a>

  (datastore DatastoreServiceConfig$Builder/withDefaults)
  ; => #<DatastoreServiceImpl DatastoreServiceImpl@a7b68a>"
  [ & [configuration]]
  (DatastoreServiceFactory/getDatastoreService
   (or configuration (DatastoreServiceConfig$Builder/withDefaults))))

(defn current-transaction
  "Returns the current transaction, or nil if no transaction is
  active."
  [] nil)

(extend-type Entity
  Protocol
  (delete [entity]
    (delete (.getKey entity)))
  (get [entity]
    (get (.getKey entity)))
  (put [entity]   
    (.put (datastore) (current-transaction) entity)
    entity))

(extend-type Key
  Protocol
  (delete [key]
    (.delete (datastore) (current-transaction) (into-array [key])) key)
  (get [key]
    (try (.get (datastore) (current-transaction) key)
         (catch EntityNotFoundException _ nil)))
  (put [key]
    (put (Entity. key))))
