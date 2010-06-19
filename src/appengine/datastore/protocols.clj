(ns #^{:author "Roman Scherer"
       :doc "The datastore protocols."}
  appengine.datastore.protocols)

(defprotocol Record
  (create [record]
    "Create new record in the datastore. If the record already exists,
    the functions throws an exception.")
  (delete [record]
    "Delete the record from the datastore.")
  (lookup [record]
    "Lookup the record from the datastore.")
  (save [record]
    "Save the record in the datastore.")
  (update [record key-vals]
    "Update the record with the key-vals and save it in the
    datastore."))

(defprotocol Lifecycle
  (deserialize [object] "Deserialize an object from the datastore.")
  (serialize [record] "Serialize the record into an entity."))

(extend-type clojure.lang.ISeq
  Record
  (create [records] (map create records))
  (delete [records] (map delete records))
  (lookup [records] (map lookup records))
  (save   [records] (map save records))
  (update [records key-vals] (map #(update % key-vals) records)))
