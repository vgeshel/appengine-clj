(ns appengine.datastore.test.protocols
  (:import (com.google.appengine.api.datastore Entity))
  (:use appengine.datastore.entities
        appengine.datastore.keys
        appengine.datastore.protocols
        appengine.test
        clojure.test))

(defn make-sequence []
  [(make-key "continent" "eu")
   (Entity. (make-key "continent" "af"))])

(datastore-test test-create
  (let [entities (create (make-sequence))]
    (is (seq? entities))))

(datastore-test test-delete
  (let [entities (create (make-sequence))]
    (is (delete entities))))

(datastore-test test-lookup
  (let [entities (create (make-sequence))]
    (is (lookup entities))))

(datastore-test test-save
  (let [entities (create (make-sequence))]
    (is (save entities))))

(datastore-test test-update
  (let [entities (create (make-sequence))]
    (is (update entities {:updated-at "TIMESTAMP"}))))
