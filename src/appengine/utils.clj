(ns #^{:author "Roman Scherer"
       :doc "Utility functions for Google App Engine." }
  appengine.utils)

(defn compact [seq]
  (remove nil? seq))

(defn map-keys
  "Returns a lazy sequence consisting of the result of applying f to
  the keys of coll."
  [coll f] (zipmap (map f (keys coll)) (vals coll)))

(defn map-keyword [coll]
  "Returns a lazy sequence consisting of the result of applying
  #'keyword to the keys of coll."
  (map-keys coll #'keyword))
