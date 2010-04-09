(ns appengine.test-utils
  (:require [clojure.contrib.test-is :as test-is])
  (:import
    (com.google.appengine.tools.development ApiProxyLocalFactory
					    LocalServerEnvironment)
    (com.google.appengine.api.datastore.dev LocalDatastoreService)
    (com.google.apphosting.api ApiProxy)))

(defn refer-private [ns]
  (doseq [[symbol var] (ns-interns ns)]
    (when (:private (meta var))
      (intern *ns* symbol var))))

(defn ds-setup []
  (let [proxy-factory (ApiProxyLocalFactory.)
	environment 
	(proxy [LocalServerEnvironment] [] 
	  (getAppDir [] (java.io.File. "."))) 
	api-proxy (.create proxy-factory environment)]
    (.setProperty api-proxy LocalDatastoreService/NO_STORAGE_PROPERTY
		  "true")
    (ApiProxy/setDelegate api-proxy))
  (ApiProxy/setEnvironmentForCurrentThread
   (proxy [com.google.apphosting.api.ApiProxy$Environment] []
     (getAppId [] "test")
     (getVersionId [] "1.0")
     (getRequestNamespace [] "")
     (getAttributes [] (java.util.HashMap.)))))

(defn ds-teardown []
  (ApiProxy/clearEnvironmentForCurrentThread)
  (.stop (ApiProxy/getDelegate)))

(defmacro dstest [name & body]
  `(test-is/deftest ~name
    (ds-setup)
    (try
      ~@body
      (finally (ds-teardown)))))

