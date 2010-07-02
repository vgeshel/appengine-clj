(ns #^{:author "Roman Scherer"}
  appengine.server
  (:import org.mortbay.jetty.handler.ContextHandlerCollection
           org.mortbay.jetty.Server
           javax.servlet.http.HttpServlet
           [org.mortbay.jetty.servlet Context ServletHolder])
  (:use appengine.environment
        ;; [appengine.jetty :only (run-jetty)]
        [ring.util.servlet :only (servlet)]
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
