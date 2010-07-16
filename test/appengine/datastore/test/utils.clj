(ns appengine.datastore.test.utils
  (:import (com.google.appengine.api.datastore Entity))
  (:use appengine.datastore.utils
        appengine.datastore
        appengine.test
        clojure.test))

(datastore-test test-assert-new
  (let [entity {:kind "continent"}]
    (is (= (assert-new entity) entity))
    (is (thrown? Exception (assert-new (create-entity entity))))))
