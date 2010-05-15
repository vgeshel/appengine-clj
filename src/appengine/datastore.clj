(ns #^{:author "Roman Scherer with contributions by John D. Hume,
Jean-Denis Greze."
       :doc "Clojure API for the Google App Engine datastore service." }
  appengine.datastore
  (:use [clojure.contrib.ns-utils :only (immigrate)]))

(immigrate
 'appengine.datastore.core
 'appengine.datastore.entities
 'appengine.datastore.transactions)

