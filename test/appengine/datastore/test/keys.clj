(ns appengine.datastore.test.keys
  (:import (com.google.appengine.api.datastore Entity Key KeyFactory))
  (:use appengine.datastore.entities
        appengine.datastore.keys
        appengine.datastore.protocols
        appengine.test
        clojure.test
        [clojure.contrib.string :only (lower-case)]))

(defn make-europe []
  (make-key "continent" "eu"))

(defn create-europe []
  (save (make-europe)))

(datastore-test test-create
  (let [entity (create (make-europe))]
    (is (map? entity))
    (is (= (lookup entity) entity))
    (is (thrown? Exception (create (.getKey entity))))))

(datastore-test test-delete
  (let [entity (create-europe)]
    (is (map? entity))
    (is (= (lookup entity) entity))
    (delete (:key entity))
    (is (nil? (lookup entity)))))

(datastore-test test-save
  (testing "with complete key"
    (let [entity (save (make-europe))]
      (is (map? entity))
      (is (= entity (lookup entity)))))
  (testing "with incomplete key"
    (let [entity (save (doto (Entity. "person") (.setProperty "name" "Bob")))]
      (is (map? entity))
      (is (= entity (lookup entity))))))

(datastore-test test-lookup
  (is (nil? (lookup (make-europe))))
  (is (not (nil? (lookup (:key (create-europe))))))
  (let [entity (create-europe)]
    (is (map? entity))
    (is (= (lookup (:key entity)) entity))))

(datastore-test test-update
  (testing "with saved key"    
    (let [entity (update (:key (create-europe)) {:name "Asia"})]
      (is (map? entity))
      (is (= (:name entity) "Asia"))))
  (testing "with unsaved key"    
    (let [entity (update (make-europe) {:name "Asia"})]
      (is (map? entity))
      (is (= (:name entity) "Asia")))))

(datastore-test test-make-key-with-int
  (let [key (make-key "continent" 1)]
    (is (key? key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))    
    (is (= (.getKind key) "continent"))
    (is (= (.getId key) 1))
    (is (nil? (.getName key)))))

(datastore-test test-make-key-with-string
  (let [key (make-key "country" "de")]
    (is (key? key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))
    (is (= (.getKind key) "country"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de"))))

(datastore-test test-make-key-with-parent
  (let [continent (make-key "continent" "eu")
        country (make-key continent "country" "de")]
    (is (= (class country) Key))
    (is (.isComplete country))
    (is (= (.getParent country) continent))
    (is (= (.getKind country) "country"))
    (is (= (.getId country) 0))
    (is (= (.getName country) "de"))))

(datastore-test test-key?
  (is (not (key? nil)))
  (is (not (key? "")))
  (is (key? (make-key "continent" "eu"))))

(datastore-test test-key->string  
  (is (= (key->string (make-key "continent" 1)) "agR0ZXN0cg8LEgljb250aW5lbnQYAQw"))
  (is (= (key->string (make-key "country" "de")) "agR0ZXN0cg8LEgdjb3VudHJ5IgJkZQw"))
  (let [continent (make-key "continent" "eu")
        country (make-key continent "country" "de")]
    (is (= (key->string country) "agR0ZXN0ciALEgljb250aW5lbnQiAmV1DAsSB2NvdW50cnkiAmRlDA"))))

(datastore-test test-string->key
  (let [key (string->key "agR0ZXN0cg8LEgljb250aW5lbnQYAQw")]
    (is (= (.getKind key) "continent"))
    (is (= (.getId key) (long 1)))
    (is (nil? (.getName key))))
  (let [key (string->key "agR0ZXN0cg8LEgdjb3VudHJ5IgJkZQw")]
    (is (= (.getKind key) "country"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de")))
  (let [key (string->key "agR0ZXN0ciALEgljb250aW5lbnQiAmV1DAsSB2NvdW50cnkiAmRlDA")]
    (is (= (.getKind key) "country"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de"))
    (let [parent (.getParent key)]
      (is (= (.getKind parent) "continent"))
      (is (= (.getId parent) 0))
      (is (= (.getName parent) "eu")))))
