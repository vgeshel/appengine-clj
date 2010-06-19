(ns appengine.datastore.test.transactions
  (:import (com.google.appengine.api.datastore Entity KeyFactory))
  (:use clojure.test appengine.datastore.transactions appengine.test)
  (:require [appengine.datastore.service :as datastore]))

(defn example-entity []
  (Entity. (KeyFactory/createKey "continent" "eu")))

(datastore-test test-active?
  (let [transaction (datastore/begin-transaction)]
    (is (active? transaction))
    (commit transaction)
    (is (not (active? transaction))))
  (let [transaction (datastore/begin-transaction)]
    (is (active? transaction))
    (rollback transaction)
    (is (not (active? transaction)))))

(datastore-test test-commit
  (let [transaction (datastore/begin-transaction)
        entity (datastore/put (example-entity))]
    (is (nil? (datastore/get entity)))
    (commit transaction)
    (is (= (datastore/get entity) entity))))

(datastore-test test-rollback
  (let [transaction (datastore/begin-transaction)
        entity (datastore/put (example-entity))]
    (rollback transaction)
    (is (nil? (datastore/get entity)))))

(datastore-test test-transaction?
  (is (not (transaction? nil)))
  (is (not (transaction? "")))
  (let [transaction (datastore/begin-transaction)]
    (is (transaction? transaction))
    (commit transaction)))

(datastore-test test-with-commit-transaction
  (let [entity (with-commit-transaction (datastore/put (example-entity)))]
    (is (= (datastore/get entity) entity))))

(datastore-test test-with-rollback-transaction
  (let [exception (Exception. "datastore exception")]
    (try
      (with-rollback-transaction
        (datastore/put (example-entity))
        (throw exception))
      (catch Exception catched
        (is (= catched exception))
        (is (nil? (datastore/get (example-entity))))))))
