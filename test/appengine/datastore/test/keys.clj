(ns appengine.datastore.test.keys
  (:use appengine.datastore.keys
        appengine.test
        clojure.test
        [clojure.contrib.string :only (lower-case)])
  (:import (com.google.appengine.api.datastore Key KeyFactory)))

(datastore-test test-create-key-with-int
  (let [key (create-key "continent" 1)]
    (is (key? key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))    
    (is (= (.getKind key) "continent"))
    (is (= (.getId key) 1))
    (is (nil? (.getName key)))))

(datastore-test test-create-key-with-string
  (let [key (create-key "country" "de")]
    (is (key? key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))
    (is (= (.getKind key) "country"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de"))))

(datastore-test test-create-key-with-parent
  (let [continent (create-key "continent" "eu")
        country (create-key continent "country" "de")]
    (is (= (class country) Key))
    (is (.isComplete country))
    (is (= (.getParent country) continent))
    (is (= (.getKind country) "country"))
    (is (= (.getId country) 0))
    (is (= (.getName country) "de"))))

(datastore-test test-key?
  (is (not (key? nil)))
  (is (not (key? "")))
  (is (key? (create-key "continent" "eu"))))

(datastore-test test-key->string  
  (is (= (key->string (create-key "continent" 1)) "agR0ZXN0cg8LEgljb250aW5lbnQYAQw"))
  (is (= (key->string (create-key "country" "de")) "agR0ZXN0cg8LEgdjb3VudHJ5IgJkZQw"))
  (let [continent (create-key "continent" "eu")
        country (create-key continent "country" "de")]
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
