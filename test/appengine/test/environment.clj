(ns appengine.test.environment
  (:import (com.google.apphosting.api ApiProxy ApiProxy$Environment)
           (com.google.appengine.tools.development ApiProxyLocalFactory LocalServerEnvironment))
  (:use clojure.test appengine.environment))

(deftest test-login-aware-proxy
  (let [proxy (login-aware-proxy {})]
    (is (isa? (class proxy) ApiProxy$Environment))
    (is (= (.getAppId proxy) "local"))))

(deftest test-local-server-environment
  (let [environment (local-server-environment)]
    (is (isa? (class environment) LocalServerEnvironment))
    (is (= (.getAppDir environment) (java.io.File. (System/getProperty "java.io.tmpdir")))))
  (let [environment (local-server-environment ".")]
    (is (isa? (class environment) LocalServerEnvironment))
    (is (= (.getAppDir environment) (java.io.File. ".")))))

(deftest test-init-appengine
  (let [proxy (init-appengine)]
    (println proxy)
    ))
