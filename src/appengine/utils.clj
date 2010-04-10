(ns appengine.utils)

(defn compact [seq]
  (remove nil? seq))
