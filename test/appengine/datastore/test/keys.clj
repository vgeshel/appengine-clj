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
    (is (= (select entity) entity))
    (is (thrown? Exception (create (.getKey entity))))))

(datastore-test test-delete
  (let [entity (create-europe)]
    (is (map? entity))
    (is (= (select entity) entity))
    (delete (:key entity))
    (is (nil? (select entity)))))

(datastore-test test-save
  (testing "with complete key"
    (let [entity (save (make-europe))]
      (is (map? entity))
      (is (= entity (select entity)))))
  (testing "with incomplete key"
    (let [entity (save (doto (Entity. "person") (.setProperty "name" "Bob")))]
      (is (map? entity))
      (is (= entity (select entity))))))

(datastore-test test-select
  (is (nil? (select (make-europe))))
  (is (not (nil? (select (:key (create-europe))))))
  (let [entity (create-europe)]
    (is (map? entity))
    (is (= (select (:key entity)) entity))))

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

(deftest test-key-name
  (let [continent {:iso-3166-alpha-2 "EU" :name "Europe" :location {:latitude 54.5260, :longitude 15.2551}}]
    (is (nil? (key-name continent)))    
    ;; (is (thrown? IllegalArgumentException (key-name continent :asas #'identity)))
    (is (= (key-name continent :iso-3166-alpha-2 #'identity) "EU"))
    (is (= (key-name continent :iso-3166-alpha-2 #'lower-case) "eu"))
    (is (= (key-name continent :iso-3166-alpha-2 #'lower-case :name #'lower-case) "eu-europe"))
    (is (= (key-name continent :name #'lower-case :iso-3166-alpha-2 #'lower-case) "europe-eu"))))
