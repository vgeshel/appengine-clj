(ns appengine.test
  (:use clojure.test)
  (:import 
   (com.google.appengine.tools.development.testing 
    LocalDatastoreServiceTestConfig
    LocalMemcacheServiceTestConfig
    LocalServiceTestHelper
    LocalTaskQueueTestConfig)
   (com.google.apphosting.api ApiProxy)))

(defn refer-private [ns]
  (doseq [[symbol var] (ns-interns ns)]
    (when (:private (meta var))
      (intern *ns* symbol var))))

(defn local-service-test-helper [& configs]
  (LocalServiceTestHelper. (into-array configs)))

;; use the hereunder as opposed to (.tear-down local-service-test-helper)
;; because the latter hangs the test framework from lein test 
(defn tear-down []
  (ApiProxy/clearEnvironmentForCurrentThread)
  (.stop (ApiProxy/getDelegate)))

(defmacro with-local-datastore [& body]
  `(try (.setUp (local-service-test-helper (LocalDatastoreServiceTestConfig.)))
        ~@body
        (finally (tear-down))))

(defmacro with-local-memcache [& body]
  `(try (.setUp (local-service-test-helper (LocalMemcacheServiceTestConfig.)))
        ~@body
        (finally (tear-down))))

(defmacro with-local-task-queue [& body]
  `(try (.setUp (local-service-test-helper (LocalTaskQueueTestConfig.)))
        ~@body
        (finally (tear-down))))

(defmacro datastore-test [name & body]
  `(deftest ~name
     (with-local-datastore ~@body)))

(defmacro datastore-test [name & body]
  `(deftest ~name
     (with-local-datastore ~@body)))

(defmacro memcache-test [name & body]
  `(deftest ~name
     (with-local-memcache ~@body)))

(defmacro task-queue-test [name & body]
  `(deftest ~name
     (with-local-task-queue ~@body)))
