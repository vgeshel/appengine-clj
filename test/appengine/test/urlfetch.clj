(ns appengine.test.urlfetch
  (:require [appengine.urlfetch :as uf])
  (:use clojure.test
        appengine.test-utils.urlfetch)
  (:import [com.google.appengine.api.urlfetch
            URLFetchServiceFactory
            URLFetchService]))

(uftest test-urlfetch-service
  (is (not (nil? (uf/urlfetch)))))

(uftest test-fetch
  (let [res (uf/fetch "http://www.google.com/")]
    (is (= (:status-code res) 200))
    (is (= (class (:headers res)) clojure.lang.PersistentArrayMap)))
  (let [res (uf/fetch "http://www.google.com/" {:follow-redirects false})]
    (is (= (:status-code res) 302)))
  (let [res (uf/fetch "http://www.google.com/" {:method "POST"})]
    (is (= (:status-code res) 405))
    (is (= (-> res :headers :Allow) "GET, HEAD"))))
