(ns #^{:author "Roman Scherer"
       :doc
       "Allows you to create arbitrary Key objects in the root
       group (no parent).

       Also allows you to encode Key objects to and decode them from
  strings. Clients should not make any assumptions about this return
  value, except that it is a websafe string that does not need to be
  quoted when used in HTML or in URLs."}
  appengine.datastore.keys
  (:import (com.google.appengine.api.datastore Entity EntityNotFoundException Key KeyFactory))
  (:use [appengine.datastore.utils :only (assert-new)]
        [clojure.contrib.string :only (join replace-re)]
        appengine.datastore.protocols)
  (:require [appengine.datastore.service :as datastore]))

(defn key?
  "Returns true if arg is a Key, else false."
  [arg] (isa? (class arg) Key))

(defn make-key
  "Creates a new Key using the given kind and identifier. If parent-key is
given, the new key will be a child of the parent-key.
  
Examples:

  (make-key \"country\" \"de\")
  ; => #<Key country(\"de\")>
 	
  (make-key (make-key \"continent\" \"eu\") \"country\" \"de\")
  ; => #<Key continent(\"eu\")/country(\"de\")>"
  ([kind identifier]
     (make-key nil kind identifier))
  ([#^Key parent-key kind identifier]
     (KeyFactory/createKey
      (if (key? parent-key) parent-key (:key parent-key))
      kind 
      (if (integer? identifier) (Long/valueOf (str identifier)) 
          (str identifier)))))

(defn key-name
  "Returns a named key from the properties map."
  [properties & key-transform-fns]
  (let [transformed-keys
        (map (fn [[key transform-fn]]
               (if-let [value (key properties)]
                 (transform-fn value)
                 (throw (IllegalArgumentException.
                         (str "Can't find key " key " in " properties ".")))))
             (partition 2 key-transform-fns))]
    (if-not (empty? transformed-keys)
      (join "-" transformed-keys))))

(defn key->string
  "Returns a \"websafe\" string from the given Key.

Examples:

  (key->string (make-key \"continent\" \"eu\"))
  ; => \"agR0ZXN0chELEgljb250aW5lbnQiAmV1DA\"

  (key->string (make-key (make-key \"continent\" \"eu\") \"country\" \"de\")
  ; => \"agR0ZXN0ciALEgljb250aW5lbnQiAmV1DAsSB2NvdW50cnkiAmRlDA\""
  [#^Key key] (KeyFactory/keyToString key))

(defn string->key
  "Returns a Key from the given \"websafe\" string.

Examples:

  (string->key \"agR0ZXN0chELEgljb250aW5lbnQiAmV1DA\")
  ; => #<Key country(\"de\")>

  (string->key \"agR0ZXN0ciALEgljb250aW5lbnQiAmV1DAsSB2NvdW50cnkiAmRlDA\")
  ; => #<Key continent(\"eu\")/country(\"de\")>"
  [string] (KeyFactory/stringToKey string))

(extend-type Key
  Record
  (create [key]
    (assert-new key)
    (save key))
  (delete [key]
    (datastore/delete-entity key))
  (save [key]
    (deserialize (datastore/put-entity (Entity. key) )))
  (select [key]          
          (try (if-let [entity (datastore/get-entity key)]
                 (deserialize entity))
         (catch EntityNotFoundException _ nil)))
  (update [key key-vals]
    (if-let [entity (datastore/get-entity key)]
      (update entity key-vals))))
