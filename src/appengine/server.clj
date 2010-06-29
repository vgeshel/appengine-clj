(ns #^{:author "Roman Scherer"}
  appengine.server
  (:use appengine.environment
        [ring.adapter.jetty :only (run-jetty)]))

(defn start-server
  "Start the server.

Example:

  (start-server \"Hello World\" :directory \"/tmp\" :join? false :port 8080)
  ; => #<Server Server@1a2cc7>
"
  [application & options]
  (let [options (apply hash-map options)
        directory (or (:directory options) "war")]
    (dosync
     (init-appengine directory)
     (run-jetty application options))))

(defn stop-server
  "Stop the server.

Example:

  (def *server* (start-server \"Hello World\" :directory \"/tmp\" :join? false :port 8080))
  ; => #<Server Server@1a2cc7>

  (stop-server *server*)
  ; => #<Server Server@1a2cc7>
"
  [server]
  (do (.stop server)
      server))

;; (def *server* (start-server "Hello World" :directory "/tmp" :join? false :port 8123))
;; *server*
;; (stop-server *server*)
