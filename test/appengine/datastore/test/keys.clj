(ns appengine.datastore.test.keys
  (:use clojure.test appengine.datastore.keys appengine.test)
  (:import (com.google.appengine.api.datastore Key KeyFactory)))

(datastore-test test-create-key-with-int
  (let [key (create-key "continent" 1)]
    (is (= (class key) Key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))    
    (is (= (.getKind key) "continent"))
    (is (= (.getId key) 1))
    (is (nil? (.getName key)))))

(datastore-test test-create-key-with-string
  (let [key (create-key "country" "de")]
    (is (= (class key) Key))
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
