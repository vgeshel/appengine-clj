(defproject appengine "0.3-SNAPSHOT"
  :author "John D. Hume, Roman Scherer, Jean-Denis Greze, 深町英太郎(E. Fukamachi)"
  :description "Clojure library for Google App Engine."
  :url "http://github.com/r0man/appengine-clj"
  :autodoc {:name "Google App Engine for Clojure"
            :copyright "Copyright (c) 2009, 2010 John D. Hume, Roman Scherer, Jean-Denis Greze, E.Fukamachi"}
  :dependencies [[com.google.appengine/appengine-api-1.0-sdk "1.3.4"]
                 [inflections "0.4-SNAPSHOT"]
                 [org.clojure/clojure "1.2.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.2.0-SNAPSHOT"]
                 [org.clojars.paraseba/ring-servlet "0.2.0"]]
  :dev-dependencies [[autodoc "0.7.1-SNAPSHOT"]
                     [lein-clojars "0.5.0"]
                     [com.google.appengine/appengine-api-labs "1.3.4"]
                     [com.google.appengine/appengine-api-stubs "1.3.4"]
                     [com.google.appengine/appengine-local-runtime "1.3.4"]
                     ;; [com.google.appengine/appengine-local-runtime-shared "1.3.4"]
                     [com.google.appengine/appengine-testing "1.3.4"]
                     [org.clojars.paraseba/ring-jetty-adapter "0.2.0"]
                     [swank-clojure "1.2.1"]]
  :repositories [["maven-gae-plugin" "http://maven-gae-plugin.googlecode.com/svn/repository"]])
