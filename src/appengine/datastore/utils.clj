(ns appengine.datastore.utils
  (:use [appengine.datastore.protocols :only (lookup)]))

(defn assert-new [entity-or-key]
  (if-let [found (lookup entity-or-key)]
    (throw (Exception. (str "Can't create entity." " Already existing: " entity-or-key)))
    entity-or-key))
