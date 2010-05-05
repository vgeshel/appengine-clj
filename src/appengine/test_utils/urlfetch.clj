(ns appengine.test-utils.urlfetch
  (:use clojure.test
        appengine.test-utils)
  (:import [com.google.appengine.tools.development.testing
            LocalURLFetchServiceTestConfig
            LocalServiceTestHelper]))

(def test-helper (get-test-helper (LocalURLFetchServiceTestConfig.)))

(defmacro uftest [name & body]
  `(deftest ~name
     (with-local-service ~'test-helper ~@body)))
