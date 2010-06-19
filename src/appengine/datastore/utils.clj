(ns appengine.datastore.utils
  (:use [appengine.datastore.protocols :only (select)]))

(defn assert-new [entity-or-key]
  (if-let [found (select entity-or-key)]
    (throw (Exception. (str "Can't create entity." " Already existing: " entity-or-key)))))
