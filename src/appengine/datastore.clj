(ns #^{:author "Roman Scherer with contributions by John D. Hume and
Jean-Denis Greze."
       :doc "Clojure API for the Google App Engine datastore service.

Examples:

  (defentity Continent ()
    ((iso-3166-alpha-2 :key lower-case :serialize lower-case)
     (location :serialize GeoPt)
     (name)))
  ; => (user.Continent)

  (def *europe* (continent :name \"Europe\" :iso-3166-alpha-2 \"eu\"))
  ; => #'user/*europe*

  (create *europe*)
  ; => #:user.Continent{:key #<Key user.Continent(\"eu\")>, :kind \"user.Continent\",
                        :iso-3166-alpha-2 \"eu\", :location nil, :name \"Europe\"}

  (defentity Country (Continent)
    ((iso-3166-alpha-2 :key lower-case :serialize lower-case)
     (location :serialize GeoPt)
     (name)))

"
  } appengine.datastore
  (:use [appengine.utils :only (immigrate-symbols)]))

(immigrate-symbols
 'appengine.datastore.entities 'defentity)

(immigrate-symbols
 'appengine.datastore.keys 'key? 'make-key 'key->string 'string->key)

(immigrate-symbols
 'appengine.datastore.protocols
 'create 'delete 'lookup 'save 'update)

(immigrate-symbols
 'appengine.datastore.query 'select 'query?)

(immigrate-symbols
 'appengine.datastore.transactions
 'active? 'commit 'rollback 'with-commit-transaction 'with-rollback-transaction)
