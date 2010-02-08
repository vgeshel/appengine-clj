(ns appengine-clj.datastore
  (:import (com.google.appengine.api.datastore
            DatastoreServiceFactory Entity Key Query KeyFactory))
  (:refer-clojure :exclude [get]))

(defn create-key
  "Creates a new Key from the given kind and id. If a parent key is given
the new key will be a child of the parent key."
  ([kind id]
     (create-key nil kind id))
  ([#^Key parent kind id]
     (KeyFactory/createKey parent kind (if (integer? id) (Long/valueOf (str id)) (str id)))))

(defn key->string
  "Converts the given Key into a websafe string."
  [key] (KeyFactory/keyToString key))

(defn string->key
  "Converts a String-representation of a Key into the Key instance it represents."
  [string] (KeyFactory/stringToKey string))

(defn entity->map
  "Converts an instance of com.google.appengine.api.datastore.Entity
  to a PersistentHashMap with properties stored under keyword keys,
  plus the entity's kind stored under :kind and key stored under :key."
  [#^Entity entity]
  (reduce #(assoc %1 (keyword (key %2)) (val %2))
    {:kind (.getKind entity) :key (.getKey entity)}
    (.entrySet (.getProperties entity))))

(defn map->entity
  "Converts a PersistentHashMap into a Entity instance. The map must
have the key or kind of the entity stored under the :key or a :kind
keywords."
  [map]
  (reduce #(do (.setProperty %1 (name (first %2)) (second %2)) %1)
          (Entity. (or (:key map) (:kind map))) (dissoc map :key :kind)))

(defn get
  "Retrieves the identified entity or raises EntityNotFoundException."
  [#^Key key]
  (entity->map (.get (DatastoreServiceFactory/getDatastoreService) key)))

;; (defn put [map]
;;   (.put (DatastoreServiceFactory/getDatastoreService)
;;         (map->entity map)))

(defn put [map]
  (assoc map :key (.put (DatastoreServiceFactory/getDatastoreService) (map->entity map))))

;; (defn create-key [kind id]
;;   (KeyFactory/createKey
;;    kind (String/valueOf id)))


(defn find-all
  "Executes the given com.google.appengine.api.datastore.Query
  and returns the results as a lazy sequence of items converted with entity->map."
  [#^Query query]
  (let [data-service (DatastoreServiceFactory/getDatastoreService)
        results (.asIterable (.prepare data-service query))]
    (map entity->map results)))

(defn create
  "Takes a map of keyword-value pairs or struct and puts a new Entity in the Datastore.
  The map or struct must include a :kind String.
  Returns the saved Entity converted with entity->map (which will include the assigned :key)."
  ([item] (create item nil))
  ([item #^Key parent-key]
    (let [kind (item :kind)
          properties (dissoc (merge {} item) :kind) ; converts struct to map
          entity (if parent-key (Entity. kind parent-key) (Entity. kind))]
      (doseq [[prop-name value] properties]
        (.setProperty entity (name prop-name) value))
      (let [key (.put (DatastoreServiceFactory/getDatastoreService) entity)]
        (assoc (entity->map entity) :key key)))))

;; (defn create
;;   "Takes a map of keyword-value pairs or struct and puts a new Entity in the Datastore.
;;   The map or struct must include a :kind String.
;;   Returns the saved Entity converted with entity->map (which will include the assigned :key)."
;;   ([item] (create item nil))
;;   ([item #^Key parent-key]
;;      (put (assoc item :key (if parent-key (Entity. (:kind item) parent-key) (Entity. (:kind item)))))))

(defn update
  "Takes a map of properties and updates or adds to the identified Entity"
  [properties #^Key key]
  (let [entity (.get (DatastoreServiceFactory/getDatastoreService) key)]
    (doseq [[prop-name value] properties] (.setProperty entity (name prop-name) value))
    (.put (DatastoreServiceFactory/getDatastoreService) entity)
    (entity->map entity)))

(defn delete
  "Deletes the identified entities."
  [& #^Key keys]
  (.delete (DatastoreServiceFactory/getDatastoreService) keys))

