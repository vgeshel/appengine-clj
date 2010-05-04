(ns appengine.test-utils.datastore
  (:use clojure.test appengine.test-utils)
  (:import [com.google.appengine.tools.development.testing
            LocalDatastoreServiceTestConfig
            LocalServiceTestHelper]))

(defn refer-private [ns]
  (doseq [[symbol var] (ns-interns ns)]
    (when (:private (meta var))
      (intern *ns* symbol var))))

(def test-helper (get-test-helper (LocalDatastoreServiceTestConfig.)))

(defmacro dstest [name & body]
  `(deftest ~name
     (with-local-service ~'test-helper ~@body)))
