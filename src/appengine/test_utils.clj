(ns appengine.test-utils
  (:use clojure.test)
  (:import 
   (com.google.appengine.tools.development.testing 
    LocalDatastoreServiceTestConfig
    LocalServiceTestHelper)
   (com.google.apphosting.api ApiProxy)))

(defn refer-private [ns]
  (doseq [[symbol var] (ns-interns ns)]
    (when (:private (meta var))
      (intern *ns* symbol var))))

;; only create one testhelper for all tests, which we repeatedly
;; clear using AppEngine setup/teardown semantics
(def test-helper 
     (let [helper (LocalServiceTestHelper. 
		   (into-array [(LocalDatastoreServiceTestConfig.)]))]
       helper))

;; use the hereunder as opposed to (.tearDown test-helper)
;; because the latter hangs the test framework from lein test 
(defn teardown []
  (ApiProxy/clearEnvironmentForCurrentThread)
  (.stop (ApiProxy/getDelegate)))

(defmacro dstest [name & body]
  `(deftest ~name
     (try (.setUp test-helper)
	  ~@body
	  (finally (teardown)))))



