(ns appengine.datastore.test.keys
  (:import (com.google.appengine.api.datastore Key KeyFactory))
  (:use appengine.datastore.entities
        appengine.datastore.keys
        appengine.datastore.protocols
        appengine.test
        clojure.test
        [clojure.contrib.string :only (lower-case)]))

(defn make-europe []
  (make-key "continent" "eu"))

(defn create-europe []
  (save-entity (make-europe)))

(datastore-test test-create-entity
  (let [entity (create-entity (make-europe))]
    (is (map? entity))
    (is (= (find-entity entity) entity))
    (is (thrown? Exception (create-entity (.getKey entity))))))

(datastore-test test-delete-entity
  (let [entity (create-europe)]
    (is (map? entity))
    (is (= (find-entity entity) entity))
    (delete-entity (:key entity))
    (is (nil? (find-entity entity)))))

(datastore-test test-save-entity
  (testing "with complete key"
    (let [entity (save-entity (make-europe))]
      (is (map? entity))
      (is (= entity (find-entity entity)))))
  (testing "with incomplete key"
    (let [entity (save-entity (doto (com.google.appengine.api.datastore.Entity. "person")
                                (.setProperty "name" "Bob")))]
      (is (map? entity))
      (is (= entity (find-entity entity))))))

(datastore-test test-find-entity
  (is (nil? (find-entity (make-europe))))
  (is (not (nil? (find-entity (:key (create-europe))))))
  (let [entity (create-europe)]
    (is (map? entity))
    (is (= (find-entity (:key entity)) entity))))

(datastore-test test-update-entity-entity
  (testing "with saved key"    
    (let [entity (update-entity (:key (create-europe)) {:name "Asia"})]
      (is (map? entity))
      (is (= (:name entity) "Asia"))))
  (testing "with unsaved key"    
    (let [entity (update-entity (make-europe) {:name "Asia"})]
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
