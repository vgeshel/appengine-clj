(ns appengine.datastore
  (:use [clojure.contrib.ns-utils :only (immigrate)]))

(immigrate
 'appengine.datastore.core
 'appengine.datastore.entities
 'appengine.datastore.transactions)

