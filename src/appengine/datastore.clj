(ns #^{:author "Roman Scherer with contributions by John D. Hume,
Jean-Denis Greze."
       :doc "Clojure API for the Google App Engine datastore service." }
  appengine.datastore
  (:use [appengine.utils :only (immigrate-symbols)]))

(immigrate-symbols
 'appengine.datastore.entities 'defentity)

(immigrate-symbols
 'appengine.datastore.keys 'key? 'make-key 'key->string 'string->key)

(immigrate-symbols
 'appengine.datastore.protocols
 'create 'delete 'lookup 'save 'update)

(immigrate-symbols
 'appengine.datastore.query 'select 'query?)

(immigrate-symbols
 'appengine.datastore.transactions
 'active? 'commit 'rollback 'with-commit-transaction 'with-rollback-transaction)
