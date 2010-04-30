(ns appengine.test-utils.memcache
  (:use clojure.test
        appengine.test-utils)
  (:import [com.google.appengine.tools.development.testing
            LocalMemcacheServiceTestConfig
            LocalServiceTestHelper]))

(def test-helper
     (LocalServiceTestHelper.
      (into-array [(LocalMemcacheServiceTestConfig.)])))

(defmacro mctest [name & body]
  `(deftest ~name
     (with-local-service ~'test-helper ~@body)))
