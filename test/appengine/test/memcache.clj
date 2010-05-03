(ns appengine.test.memcache
  (:require [appengine.memcache :as mc])
  (:use clojure.test
        appengine.test-utils.memcache)
  (:import [com.google.appengine.api.memcache
            Expiration
            MemcacheServiceFactory
            MemcacheService]))

(mctest test-memcache-service
  (is (not (nil? (mc/memcache)))))

(mctest test-put
  (mc/put-value "age" 22)
  (is (= (mc/get-value "age") 22)))

(mctest test-set
  (mc/put-value "age" 22)
  (is (= (mc/get-value "age") 22))
  (mc/set-value "age" 8000)
  (is (= (mc/get-value "age") 8000)))

(mctest test-add
  (mc/put-value "age" 22)
  (is (= (mc/get-value "age") 22))
  (mc/add-value "age" 8000)
  (is (= (mc/get-value "age") 22)))

(mctest test-replace
  (mc/put-value "age" 22)
  (is (= (mc/get-value "age") 22))
  (mc/replace-value "age" 8000)
  (is (= (mc/get-value "age") 8000))
  (mc/replace-value "sex" "male")
  (is (nil? (mc/get-value "sex"))))

(mctest test-delete
  (mc/put-value "age" 22)
  (is (= (mc/get-value "age") 22))
  (mc/delete-value "age" 0)
  (is (nil? (mc/get-value "age"))))

(mctest test-inc-and-dec
  (mc/put-value "age" 22)
  (is (mc/get-value "age") 22)
  (mc/inc-value "age")
  (is (mc/get-value "age") 23)
  (mc/dec-value "age")
  (is (mc/get-value "age") 22))

(mctest test-get-expiration
  (is (= (mc/get-expiration nil) nil))
  (is (= (mc/get-expiration 0) nil))
  (let [exp (Expiration/byDeltaMillis 1000)]
    (is (= (mc/get-expiration exp) exp)))
  (let [date (java.util.Date. 2009 10 3)
        exp (mc/get-expiration date)]
    (is (= (.getMillisecondsValue exp) (.getTime date))))
  (let [delay 10
        exp (.getSecondsValue (mc/get-expiration delay))
        unix (+ delay (int (/ (System/currentTimeMillis) 1000)))]
    (is (= exp unix)))
  (let [delay (+ 10 (* 86400 30))
        exp (.getSecondsValue (mc/get-expiration delay))
        unix (+ delay (int (/ (System/currentTimeMillis) 1000)))]
    (is (= exp unix)))
  (is (= (class (mc/get-expiration 10)) com.google.appengine.api.memcache.Expiration)))
