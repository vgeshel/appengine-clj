(ns #^{:author "Roman Scherer with contributions by John D. Hume,
Jean-Denis Greze and E.Fukamachi"
       :doc "Clojure API for the App Engine datastore." }
  appengine.datastore
  (:use [clojure.contrib.ns-utils :only (immigrate)]))

(immigrate
 'appengine.datastore.core
 'appengine.datastore.entities
 'appengine.datastore.transactions)

