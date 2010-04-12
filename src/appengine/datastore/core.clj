(ns appengine.datastore.core
  (:import (com.google.appengine.api.datastore
            DatastoreConfig DatastoreServiceFactory 
	    Entity Key Query KeyFactory)))

;; Bound per-thread as part of the dotransaction macro using binding
;; Do not change this value directly except if you know what you are
;; doing. If you are using this manually, after you are done with a 
;; transaction, make sure this is nil.
(def *thread-local-transaction* nil)

(defn datastore
  "Creates a DatastoreService using the default or the provided
configuration."
  ([] (datastore DatastoreConfig/DEFAULT))
  ([configuration] (DatastoreServiceFactory/getDatastoreService configuration)))

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
  "Converts a String-representation of a Key into the Key instance 
   it represents."
  [string] (KeyFactory/stringToKey string))

(defn entity->map
  "Converts an instance of com.google.appengine.api.datastore.Entity
  to a PersistentHashMap with properties stored under keyword keys,
  plus the entity's kind stored under :kind, key stored under :key, 
  and the key of the entity's parent, if any, under :parent-key."
  [#^Entity entity]
  (reduce #(assoc %1 (keyword (key %2)) (val %2))
	  (merge {:kind (.getKind entity) :key (.getKey entity)}
		 (if (.. entity getKey getParent)
		   {:parent-key (.. entity getKey getParent)} {}))
	  (.entrySet (.getProperties entity))))

(declare properties)

(defn map->entity
  "Converts a PersistentHashMap or struct into a Entity instance. The
   map must have the key or kind of the entity stored under the :key or
   a :kind keywords.  If the map has a :parent-key Key, the Entity instance
   will be a child of the Entity with the :parent-key Key."
  [map]
  (reduce #(do (.setProperty %1 (name (first %2)) (second %2)) %1)
		       (if (and (:kind map) (:parent-key map))
			 (Entity. (:kind map) (:parent-key map))
			 (Entity. (or (:key map) (:kind map))))		    
		       (properties map)))

(defmulti properties
  "Returns the properties of the given record as a map."
  class)

(defmethod properties Entity [#^Entity entity]
  (properties (entity->map entity)))

(defmethod properties :default [map]
  (dissoc (merge {} map) :key :kind :parent-key))

(defn filter-keys [keys]
  "Takes a sequences and returns a sequence of Key objects, doing
   conversions from Entity or map (with a :key), if possible."
  (remove nil? (map #(cond (instance? Key %) %
			   (instance? Entity %) (.getKey %)
			   (instance? Key (:key %)) (:key %)) keys)))

(defmulti get-entity
  "Retrieves a PersistentHashMap of an Entity in the datastore. The
  entity can be retrieved by an instance of Key, Entity, a
  PersistentHashMap with a :key keyword, or an ISeq of keys, in which 
  case a PersistentHashMap of appengine Keys to PersistentHashmaps of
  Entities is returned."
    (fn [& args] (class (if (second args) (second args) (first args)))))

(defmethod get-entity Entity 
  ([#^Entity entity] (get-entity *thread-local-transaction* entity))
  ([transaction #^Entity entity]
     (get-entity transaction (.getKey entity))))

(defmethod get-entity Key 
  ([key] (get-entity *thread-local-transaction* key))
  ([transaction key]
     (entity->map (.get (datastore) transaction key))))

(defmethod get-entity clojure.lang.PersistentVector 
  ([keys] (get-entity *thread-local-transaction* keys))
  ([transaction keys]
     (get-entity transaction (seq keys))))

(defmethod get-entity clojure.lang.ISeq 
  ([keys] (get-entity *thread-local-transaction* keys))
  ([transaction keys]
     (into {} (for [[key entity] (.get (datastore) transaction 
				       (filter-keys keys))] 
		[key (entity->map entity)]))))

(defmethod get-entity :default 
  ([map] (get-entity *thread-local-transaction* map))
  ([transaction map]
     (if-let [key (:key map)]
    (entity->map (.get (datastore) transaction key)))))

(defmulti put-entity
  "Puts the given record into the datastore and returns a
  PersistentHashMap of the record. The record must be an instance of
  Entity or PersistentHashMap."
  (fn [& args] (class (if (second args) (second args) (first args))))) 

(defmethod put-entity Entity 
  ([#^Entity entity] (put-entity *thread-local-transaction* entity))
  ([transaction #^Entity entity]
     (let [key (.put (datastore) transaction entity)]
       (assoc (entity->map entity) :key key))))

(defmethod put-entity :default 
  ([map] (put-entity *thread-local-transaction* map))
  ([transaction map] (put-entity transaction (map->entity map))))

(defn find-all
  "Executes the given com.google.appengine.api.datastore.Query
  and returns the results as a lazy sequence of items converted with entity->map."
  ([#^Query query] (find-all *thread-local-transaction* query))
  ([transaction #^Query query]
     (let [data-service (datastore)
	   results (.asIterable (.prepare data-service transaction query))]
       (map entity->map results))))

(defn create-entity 
  "Takes a map of keyword-value pairs or struct and puts a new Entity 
  in the Datastore.  The map or struct must include a :kind String. If
  the passed-in record includes a :parent-key Key, then the entity
  created will be a child of the Entity with key :parent-key.
  Returns the saved Entity converted with entity->map (which will 
  include the assigned :key)."
  ([record] (put-entity record))
  ([transaction record] (put-entity transaction record)))

(defmulti update-entity
  "Updates the record with the given properites. The record must be an
  instance of Entity, Key or PersistentHashMap. If one of the attributes'
  values is :remove, then the correponding property name is removed 
  from that entity.  If one of the attributes't values is nil, then the 
  corresponding property name is set to Java's null for that property."
  (fn [arg & args] (class (if (second args) (first args) arg))))

(defmethod update-entity Entity 
  ([#^Entity entity attributes] 
     (update-entity *thread-local-transaction* entity attributes))
  ([transaction #^Entity entity attributes]
     (doseq [[attribute value] attributes]
       (if (not= value :remove)
	 (.setProperty entity (name attribute) value)
	 (.removeProperty entity (name attribute))))
     (put-entity transaction entity)))

(defmethod update-entity Key 
  ([key attributes] 
     (update-entity *thread-local-transaction* key attributes))
  ([transaction key attributes]
     (update-entity transaction (get-entity transaction key) attributes)))

(defmethod update-entity :default 
  ([map attributes]
     (update-entity *thread-local-transaction* map attributes))
  ([transaction map attributes]
     (update-entity transaction (map->entity map) attributes)))

(defmulti delete-entity 
  "Deletes the record."
  (fn [& args] (class (if (second args) (second args) (first args)))))

(defmethod delete-entity Entity 
  ([#^Entity entity] (delete-entity *thread-local-transaction* entity))
  ([transaction #^Entity entity]
     (delete-entity transaction (.getKey entity))))

(defmethod delete-entity Key 
  ([key] (delete-entity *thread-local-transaction* key))
  ([transaction key]
     (.delete (datastore) transaction (into-array [key]))))

(defmethod delete-entity clojure.lang.PersistentVector 
  ([keys] (delete-entity *thread-local-transaction* keys))
  ([transaction keys]
     (delete-entity transaction (seq keys))))

(defmethod delete-entity clojure.lang.ISeq 
  ([keys] (delete-entity *thread-local-transaction* keys))
  ([transaction keys]
     (.delete (datastore) transaction (filter-keys keys))))

(defmethod delete-entity :default 
  ([map] (delete-entity *thread-local-transaction* map))
  ([transaction map]
     (if-let [key (:key map)]
       (delete-entity transaction key))))



