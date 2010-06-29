(ns appengine.test.server
  (:import org.mortbay.jetty.Server)
  (:use clojure.test appengine.server))

(def *handler* "Hello World")
(def *options* [:join? false :port 8123])

(deftest test-start-server
  (let [server (apply start-server *handler* *options*)]
    (is (isa? (class server) Server))
    (is (.isStarted server))
    (stop-server server)))

(deftest test-stop-server
  (let [server (apply start-server *handler* *options*)]
    (is (isa? (class (stop-server server)) Server))
    (is (not (.isStarted server)))))
