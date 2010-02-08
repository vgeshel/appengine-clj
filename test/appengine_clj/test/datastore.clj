(ns appengine-clj.test.datastore
  (:require [appengine-clj.datastore :as ds])
  (:use clojure.contrib.test-is
        appengine-clj.test-utils)
  (:import (com.google.appengine.api.datastore
            DatastoreServiceFactory
            Entity
            EntityNotFoundException
            KeyFactory
            Query
            Query$FilterOperator
            Query$SortDirection)))

(dstest test-create-key-with-integer
  (let [key (ds/create-key "person" 1)]
    (is (= (class key) com.google.appengine.api.datastore.Key))
    (is (= (.getKind key) "person"))
    (is (= (.getId key) 1))
    (is (nil? (.getName key)))
    (is (.isComplete key))))

(dstest test-create-key-with-string
  (let [key (ds/create-key "country" "de")]
    (is (= (class key) com.google.appengine.api.datastore.Key))
    (is (= (.getKind key) "country"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de"))
    (is (.isComplete key))))

(dstest test-create-key-with-parent
  (let [continent (ds/create-key "continent" "eu")
        country (ds/create-key continent "country" "de")]
    (is (= (class country) com.google.appengine.api.datastore.Key))
    (is (= (.getParent country) continent))
    (is (= (.getKind country) "country"))
    (is (= (.getId country) 0))
    (is (= (.getName country) "de"))
    (is (.isComplete country))))

(dstest test-key->string  
  (is (= (ds/key->string (ds/create-key "person" 1)) "agR0ZXN0cgwLEgZwZXJzb24YAQw"))
  (is (= (ds/key->string (ds/create-key "country" "de")) "agR0ZXN0cg8LEgdjb3VudHJ5IgJkZQw"))
  (let [continent (ds/create-key "continent" "eu")
        country (ds/create-key continent "country" "de")]
    (is (= (ds/key->string country) "agR0ZXN0ciALEgljb250aW5lbnQiAmV1DAsSB2NvdW50cnkiAmRlDA"))))

(dstest test-string->key
  (let [key (ds/string->key "agR0ZXN0cgwLEgZwZXJzb24YAQw")]
    (is (= (.getKind key) "person"))
    (is (= (.getId key) (long 1)))
    (is (nil? (.getName key))))
  (let [key (ds/string->key "agR0ZXN0cg8LEgdjb3VudHJ5IgJkZQw")]
    (is (= (.getKind key) "country"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de")))
  (let [key (ds/string->key "agR0ZXN0ciALEgljb250aW5lbnQiAmV1DAsSB2NvdW50cnkiAmRlDA")]
    (is (= (.getKind key) "country"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de"))
    (let [parent (.getParent key)]
      (is (= (.getKind parent) "continent"))
      (is (= (.getId parent) 0))
      (is (= (.getName parent) "eu")))))

(dstest test-map->entity
  (let [entity (ds/map->entity {:key (ds/create-key "country" "de") :name "Germany"})]
    (is (= (class entity) Entity))
    (is (= (.getKind entity) "country"))
    (is (= (.. entity getKey getKind) "country"))
    (is (= (.. entity getKey getId) 0))
    (is (= (.. entity getKey getName) "de"))
    (is (= (. entity getProperty "name") "Germany")))
  (let [entity (ds/map->entity {:kind "person" :name "bob"})]
    (is (= (class entity) Entity))
    (is (= (.getKind entity) "person"))
    (is (= (.. entity getKey getId) 0))
    (is (nil? (.. entity getKey getName)))
    (is (= (. entity getProperty "name") "bob"))))

(dstest entity-to-map-converts-to-persistent-map
  (let [entity (doto (Entity. "MyKind")
                (.setProperty "foo" "Foo")
                (.setProperty "bar" "Bar"))]
    (.put (DatastoreServiceFactory/getDatastoreService) entity)
    (is (= {:foo "Foo" :bar "Bar" :kind "MyKind" :key (.getKey entity)}
           (ds/entity->map entity)))))

(dstest find-all-runs-given-query
  (.put (DatastoreServiceFactory/getDatastoreService)
        [(doto (Entity. "A") (.setProperty "code" 1) (.setProperty "name" "jim"))
         (doto (Entity. "A") (.setProperty "code" 2) (.setProperty "name" "tim"))
         (doto (Entity. "B") (.setProperty "code" 1) (.setProperty "name" "jan"))])
  (is (= ["jim" "tim"] (map :name (ds/find-all (doto (Query. "A") (.addSort "code"))))))
  (is (= ["tim" "jim"] (map :name (ds/find-all (doto (Query. "A") (.addSort "code" Query$SortDirection/DESCENDING))))))
  (is (= ["jim"] (map :name (ds/find-all (doto (Query. "A") (.addFilter "code" Query$FilterOperator/EQUAL 1))))))
  (is (= ["jan"] (map :name (ds/find-all (doto (Query. "B") (.addFilter "code" Query$FilterOperator/EQUAL 1)))))))

(dstest create-saves-and-returns-item-with-a-key
  (let [created-item (ds/create {:kind "MyKind" :name "hume" :age 31})]
    (is (not (nil? (created-item :key))))
    (let [created-entity (.get (DatastoreServiceFactory/getDatastoreService) (created-item :key))]
      (is (= "MyKind" (.getKind created-entity)))
      (is (= "hume" (.getProperty created-entity "name")))
      (is (= 31 (.getProperty created-entity "age"))))))

(dstest create-can-create-a-child-entity-from-a-parent-key
  (let [parent (ds/create {:kind "Mother" :name "mama"})
        child (ds/create {:kind "Child" :name "baby"} (parent :key))]
    (is (= (parent :key) (.getParent (child :key))))
    (is (= [child] (ds/find-all (doto (Query. "Child" (parent :key))))))))

(dstest create-with-struct
  (defstruct person :name :kind)
  (let [person (ds/create (struct person "jim" "child"))]
    (is (not (nil? (:key person))))
    (is (= (:name person) "jim"))
    (is (= (:kind person) "child"))))

(dstest get-given-a-key-returns-a-mapified-entity
  (let [key (:key (ds/create {:kind "Person" :name "cliff"}))]
    (is (= "cliff" ((ds/get key) :name)))))

(dstest test-get
  (let [country (ds/create {:key (ds/create-key "country" "de") :name "Germany" :kind "country"})]
    (is (= ((ds/get country) country)))
    (is (= ((ds/get (:key country)) country)))
    (is (= ((ds/get (ds/map->entity country)) country)))))

(dstest test-put
  (let [person (ds/put {:name "Bob" :kind "person"})]
    (is (= (:key person) (ds/create-key "person" 1)))
    (is (= (:name person) "Bob")))
  (let [country (ds/put {:key (ds/create-key "country" "de") :name "Germany"})]
    (is (= (:key country) (ds/create-key "country" "de")))
    (is (= (:name country) "Germany"))))

(dstest delete-by-key
  (let [key (:key (ds/create {:kind "MyKind"}))]
    (ds/delete key)
    (is (thrown? EntityNotFoundException (ds/get key)))))

(dstest delete-by-multiple-keys
  (let [key1 (:key (ds/create {:kind "MyKind"}))
        key2 (:key (ds/create {:kind "MyKind"}))]
    (ds/delete key1 key2)
    (are (thrown? EntityNotFoundException (ds/get _1))
         key1
         key2)))

(dstest update-with-struct
  (defstruct person :name :kind)
  (let [person (ds/update {:name "tom"} (:key (ds/create (struct person "jim" "child"))))]
    (is (not (nil? (:key person))))
    (is (= (:name person) "tom"))
    (is (= (:kind person) "child"))))

;; (dstest test-create-with-string-key
;;   (println "AAAAAAAAAAAAAAAAAAAAAA")
;;   (let [country (ds/put {:key (ds/create-key "country" "de") :kind "country" :name "Germany"})]
;;     ;; (ds/create {:key (ds/create-key "country" "de") :kind "country" :name "Germany"})
;;     ;; (ds/create {:key (ds/create-key "country" "de") :kind "country" :name "Germany"})
;;     (println "AAAAAAAAAAAAAAAAAAAAAA")
;;     (println country)
;;     (println (:key country))
;;     ;; (println (:key country))
;;     ;; (println (.getKind(:key country)))
;;     ;; (println (.getId(:key country)))
;;     ;; (println (.getName(:key country)))
;;     (println (ds/get (:key country)))
;;     ))
