(ns #^{:author "Roman Scherer"
       :doc "The datastore protocols."}
  appengine.datastore.protocols)

(defprotocol Datastore
  (create [entities]
    "Create new entities in the datastore. If one of the entities
    already exists, the functions throws an exception.")
  (delete [entities]
    "Delete the keys or entities from the datastore.")
  (select [entities]
    "Select entities from the datastore.")
  (save [entities]
    "Save the entities in the datastore.")
  (update [entities key-vals]
    "Update the entities with the key-vals and save them in the
    datastore."))

(defprotocol Deserialize
  (deserialize [object] "Deserialize an object from the datastore."))

(defprotocol Serialize
  (serialize [record] "Serialize the datastore record into an entity."))
