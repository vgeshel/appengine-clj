(ns appengine.test-utils
  (:use clojure.test)
  (:import [com.google.apphosting.api ApiProxy]
           [com.google.appengine.tools.development.testing LocalServiceTestHelper]))

;; use the hereunder as opposed to (.tearDown test-helper)
;; because the latter hangs the test framework from lein test
(defn teardown []
  (ApiProxy/clearEnvironmentForCurrentThread)
  (.stop (ApiProxy/getDelegate)))

(defn get-test-helper [config]
  (LocalServiceTestHelper. (into-array [config])))

(defmacro with-local-service [test-helper & body]
  `(try (.setUp ~test-helper)
        ~@body
        (finally (teardown))))
