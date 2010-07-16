
# Clojure library for Google App Engine

This library is a [Clojure](http://clojure.org) wrapper for [Google
App Engine](http://code.google.com/appengine). It is based on John
Hume's [appengine-clj](http://github.com/duelinmarkers/appengine-clj)
with some enhancements. It is heavily refactored, so don't expect any
backwards compatibility with the original library.

Documentation
-------------

The [Autodoc](http://tomfaulhaber.github.com/autodoc) generated API
documentation can be found
[here](http://r0man.github.com/appengine-clj).

Installation
------------

The easiest way to use this library in your own projects is via
[Leiningen](http://github.com/technomancy/leiningen). Add the
following dependency to your project.clj file:

    [appengine "0.4-SNAPSHOT"]

To build from source, run the following commands:

    lein deps
    lein jar

Examples
--------

    ; Use the datastore api.
    (use 'appengine.datastore)

    ; Define some entities.
    (defentity Continent ()
      ((iso-3166-alpha-2 :key clojure.contrib.string/lower-case)
       (location :serialize com.google.appengine.api.datastore.GeoPt)
       (name)))

    (defentity Country (Continent)
      ((iso-3166-alpha-2 :key clojure.contrib.string/lower-case)
       (location :serialize com.google.appengine.api.datastore.GeoPt)
       (name)))

    ; Initialize the environment for the repl.
    (appengine.environment/init-repl)

    ; Make a continent record.    
    (def *europe* (continent {:iso-3166-alpha-2 "eu" :name "Europe" :location {:latitude 1 :longitude 2}}))

    ; Make a country record (a country must have a continent as it's parent).
    (def *germany* (country *europe* {:iso-3166-alpha-2 "de" :name "Germany" :location {:latitude 1 :longitude 2}}))

    ; Lookup a contient by key (returns nil, because the continent has not been saved yet).  
    (lookup *europe*)

    ; Save the continent to the datastore.
    (save *europe*)

    ; Find all continents.
    (find-continents "Europe")

    ; Find all continents by name.
    (find-continents-by-name "Europe")

    ; Delete the entity from the datastore.
    (delete *europe*)

Take a look at the
[documentation](http://r0man.github.com/appengine-clj) or browse the
test directory for more examples.
