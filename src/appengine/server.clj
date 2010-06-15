(ns #^{:author "Roman Scherer"}
  appengine.server
  (:use appengine.environment
        [ring.adapter.jetty :only (run-jetty)]))

(defn start-server
  "Start the server."
  [application & options]
  (dosync
   (init-appengine)     
   (apply run-jetty application options)))

(defn stop-server
  "Stop the server."
  [server]
  (do (.stop server)
      server))
