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

(datastore-test test-create-entity
  (let [entities (create-entity (make-sequence))]
    (is (seq? entities))))

(datastore-test test-delete-entity
  (let [entities (create-entity (make-sequence))]
    (is (delete-entity entities))))

(datastore-test test-find-entity
  (let [entities (create-entity (make-sequence))]
    (is (find-entity entities))))

(datastore-test test-save-entity
  (let [entities (create-entity (make-sequence))]
    (is (save-entity entities))))

(datastore-test test-update-entity
  (let [entities (create-entity (make-sequence))]
    (is (update-entity entities {:updated-at "TIMESTAMP"}))))
