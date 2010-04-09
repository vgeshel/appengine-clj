(ns appengine.test.datastore
  (:require [appengine.datastore :as ds])
  (:use clojure.test
	appengine.datastore.entities
        appengine.test-utils)
  (:import (com.google.appengine.api.datastore
            DatastoreServiceFactory
            Entity
            EntityNotFoundException
            KeyFactory
            Query
            Query$FilterOperator
            Query$SortDirection)))

(dstest test-create-key-with-int
  (let [key (ds/create-key "person" 1)]
    (is (= (class key) com.google.appengine.api.datastore.Key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))    
    (is (= (.getKind key) "person"))
    (is (= (.getId key) 1))
    (is (nil? (.getName key)))
    ))

(dstest test-create-key-with-string
  (let [key (ds/create-key "country" "de")]
    (is (= (class key) com.google.appengine.api.datastore.Key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))
    (is (= (.getKind key) "country"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de"))))

(dstest test-create-key-with-parent
  (let [continent (ds/create-key "continent" "eu")
        country (ds/create-key continent "country" "de")]
    (is (= (class country) com.google.appengine.api.datastore.Key))
    (is (.isComplete country))
    (is (= (.getParent country) continent))
    (is (= (.getKind country) "country"))
    (is (= (.getId country) 0))
    (is (= (.getName country) "de"))))

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
  (let [entity (ds/map->entity {:kind "person" :name "Bob"})]
    (is (= (class entity) Entity))
    (is (= (.getKind entity) "person"))
    (is (= (.. entity getKey getId) 0))
    (is (nil? (.. entity getKey getName)))
    (is (= (. entity getProperty "name") "Bob"))))

(dstest create-with-struct
  (defstruct country :name :kind)
  (let [country (ds/create (struct country "Germany" "country"))]
    (is (not (nil? (:key country))))
    (is (= (:name country) "Germany"))
    (is (= (:kind country) "country"))))

(dstest test-create-with-int-key
  (let [person (ds/create {:kind "person" :name "Bob"})]
    (is (= (class person) clojure.lang.PersistentArrayMap))
    (is (.isComplete (:key person)))
    (is (= (.getKind (:key person)) "person"))
    (is (= (.getId (:key person)) 1))
    (is (nil? (.getName (:key person))))      
    (is (= (:kind person)) "person")
    (is (= (:name person) "Bob"))))

(dstest test-create-with-string-key
  (let [country (ds/create {:key (ds/create-key "country" "de") :name "Germany"})]
    (is (= (class country) clojure.lang.PersistentArrayMap))
    (is (.isComplete (:key country)))
    (is (= (.getKind (:key country)) "country"))
    (is (= (.getId (:key country)) 0))
    (is (= (.getName (:key country)) "de"))    
    (is (= (:kind country)) "country")
    (is (= (:name country) "Germany"))))

(dstest test-get
  (is (nil? (ds/get nil)))
  (is (nil? (ds/get {}))))

(dstest test-get-with-int-key
  (let [person (ds/create {:kind "person" :name "Bob"})]
    (is (= (class person) clojure.lang.PersistentArrayMap))
    (is (= ((ds/get person) person)))
    (is (= ((ds/get (:key person)) person)))
    (is (= ((ds/get (ds/map->entity person)) person)))))

(dstest test-get-with-string-key
  (let [country (ds/create {:key (ds/create-key "country" "de") :name "Germany"})]
    (is (= (class country) clojure.lang.PersistentArrayMap))
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

(dstest delete-with-key
  (let [key (:key (ds/create {:kind "person" :name "Bob"}))]
    (ds/delete key)
    (is (thrown? EntityNotFoundException (ds/get key)))))

(dstest delete-with-multiple-keys
  (let [key1 (:key (ds/create {:kind "person" :name "Alice"}))
        key2 (:key (ds/create {:kind "person" :name "Bob"}))]
    (ds/delete key1 key2)
    (are (thrown? EntityNotFoundException (ds/get _1))
         key1 key2)))

(dstest test-update
  (let [country (ds/put {:key (ds/create-key "country" "de") :name "Deutschland"})]
    (let [country (ds/update country {:name "Germany"})]
      (is (= (:name country) "Germany")))
    (let [country (ds/update (:key country) {:name "Germany"})]
      (is (= (:name country) "Germany")))
    (let [country (ds/update (ds/map->entity country) {:name "Germany"})]
      (is (= (:name country) "Germany")))))

(dstest test-properties
  (let [record {:key (ds/create-key "person" 1) :name "Bob"}]
    (is (= (ds/properties record) {:name "Bob"}))
    (is (= (ds/properties (ds/map->entity record)) {:name "Bob"}))))

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


(dstest entity-to-map-converts-to-persistent-map
  (let [entity (doto (Entity. "MyKind")
                (.setProperty "foo" "Foo")
                (.setProperty "bar" "Bar"))]
    (.put (DatastoreServiceFactory/getDatastoreService) entity)
    (is (= {:foo "Foo" :bar "Bar" :kind "MyKind" :key (.getKey entity)
	    :entity entity}
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

;; (dstest create-can-create-a-child-entity-from-a-parent-key
;;   (let [parent (ds/create {:kind "Mother" :name "mama"})
;;         child (ds/create {:kind "Child" :name "baby"} (parent :key))]
;;     (is (= (parent :key) (.getParent (child :key))))
;;     (is (= [child] (ds/find-all (doto (Query. "Child" (parent :key))))))))

(dstest get-given-a-key-returns-a-mapified-entity
  (let [key (:key (ds/create {:kind "Person" :name "cliff"}))]
    (is (= "cliff" ((ds/get key) :name)))))

;; test for :parent and :entity modifications
(dstest make-entity-and-check-map-entity-keyword
  (let [record (ds/create {:kind "Person" :name "Andy" :age 31})
	entity1 (ds/get record)
	entity2 (ds/get record)]
    (is (= entity1 entity2))
    (is (= (:entity entity1) (:entity entity2)))
    (is (= (:entity record) (:entity entity1)))))

(dstest make-entity-with-parent
  (let [parent (ds/create {:kind "Person" :name "Andy" :age 31})
	child1 (ds/create {:kind "Child" :name "Liz" :age 5 
			   :parent (:key parent)})
	child2 (ds/create {:kind "Child" :name "Jane" :age 5
			   :parent (:key parent)})]
    (is (= (:parent child1) (:key parent)))
    (is (= (:parent child2) (:key parent)))
    (is (= (.getParent (:entity child1)) (:key parent)))))