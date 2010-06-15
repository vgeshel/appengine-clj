(ns #^{:author "Roman Scherer"
       :doc
       "Allows you to create arbitrary Key objects in the root
       group (no parent).

       Also allows you to encode Key objects to and decode them from
  strings. Clients should not make any assumptions about this return
  value, except that it is a websafe string that does not need to be
  quoted when used in HTML or in URLs."}
  appengine.datastore.keys
  (:import (com.google.appengine.api.datastore Key KeyFactory))
  (:use [clojure.contrib.string :only (join)]))

(defn key?
  "Returns true if arg is a Key, else false."
  [arg] (isa? (class arg) Key))

(defn create-key
  "Creates a new Key using the given kind and identifier. If parent-key is
given, the new key will be a child of the parent-key.
  
Examples:

  (create-key \"country\" \"de\")
  ; => #<Key country(\"de\")>
 	
  (create-key (create-key \"continent\" \"eu\") \"country\" \"de\")
  ; => #<Key continent(\"eu\")/country(\"de\")>"
  ([kind identifier]
     (create-key nil kind identifier))
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

  (key->string (create-key \"continent\" \"eu\"))
  ; => \"agR0ZXN0chELEgljb250aW5lbnQiAmV1DA\"

  (key->string (create-key (create-key \"continent\" \"eu\") \"country\" \"de\")
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
