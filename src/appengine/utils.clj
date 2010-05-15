(ns #^{:author "Roman Scherer"
       :doc "Utility functions for Google App Engine." }
  appengine.utils)

(defn compact [seq]
  (remove nil? seq))
