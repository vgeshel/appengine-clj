(ns #^{:author "Roman Scherer"
       :doc "Clojue API for Google App Engine datastore keys."}
  appengine.datastore.keys
  (:import (com.google.appengine.api.datastore Key KeyFactory)))

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
      parent-key kind 
      (if (integer? identifier) (Long/valueOf (str identifier)) 
          (str identifier)))))

(defn key->string
  "Returns a \"websafe\" string from the given Key.

Examples:

  (key->string (create-key \"continent\" \"eu\"))
  ; => \"agR0ZXN0chELEgljb250aW5lbnQiAmV1DA\"

  (key->string (create-key (create-key \"continent\" \"eu\") \"country\" \"de\")
  ; => \"agR0ZXN0ciALEgljb250aW5lbnQiAmV1DAsSB2NvdW50cnkiAmRlDA\""
  [key] (KeyFactory/keyToString key))

(defn string->key
  "Returns a Key from the given \"websafe\" string.

Examples:

  (string->key \"agR0ZXN0chELEgljb250aW5lbnQiAmV1DA\")
  ; => #<Key country(\"de\")>

  (string->key \"agR0ZXN0ciALEgljb250aW5lbnQiAmV1DAsSB2NvdW50cnkiAmRlDA\")
  ; => #<Key continent(\"eu\")/country(\"de\")>"
  [string] (KeyFactory/stringToKey string))
