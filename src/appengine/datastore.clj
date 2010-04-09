(ns appengine.datastore
  (:import (com.google.appengine.api.datastore DatastoreConfig 
					       DatastoreServiceFactory 
					       Entity Key Query KeyFactory))
  (:refer-clojure :exclude [get]))

(defn create-key
  "Creates a new Key from the given kind and id. If a parent key is given
the new key will be a child of the parent key."
  ([kind id]
     (create-key nil kind id))
  ([#^Key parent kind id]
     (KeyFactory/createKey parent kind (if (integer? id) (Long/valueOf (str id)) (str id)))))

(defn datastore
  "Creates a DatastoreService using the default or the provided
configuration."
  ([] (datastore DatastoreConfig/DEFAULT))
  ([configuration] (DatastoreServiceFactory/getDatastoreService configuration)))

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

(defmulti get
  "Retrieves a PersistentHashMap of an Entity in the datastore. The
  entity can be retrieved by an instance of Key, Entity, a
  PersistentHashMap with a :key keyword, or an ISeq of keys, in which 
  case a PersistentHashMap of appengine Keys to PersistentHashmaps of
  Entities is returned."
  class)

(defmethod get Entity [#^Entity entity]
  (get (.getKey entity)))

(defmethod get Key [key]
  (entity->map (.get (datastore) key)))

(defmethod get clojure.lang.PersistentVector [keys]
  (get (seq keys)))

(defmethod get clojure.lang.ISeq [keys]
  (into {} (for [[key entity] (.get (datastore) (filter-keys keys))] 
		 [key (entity->map entity)])))

(defmethod get :default [map]
  (if-let [key (:key map)]
    (entity->map (.get (datastore) key))))

(defmulti put
  "Puts the given record into the datastore and returns a
  PersistentHashMap of the record. The record must be an instance of
  Entity or PersistentHashMap."
  class)

(defmethod put Entity [#^Entity entity]
  (let [key (.put (datastore) entity)]
    (assoc (entity->map entity) :key key)))

(defmethod put :default [map]
  (put (map->entity map)))

(defn find-all
  "Executes the given com.google.appengine.api.datastore.Query
  and returns the results as a lazy sequence of items converted with entity->map."
  [#^Query query]
  (let [data-service (datastore)
        results (.asIterable (.prepare data-service query))]
    (map entity->map results)))

;; (defn create
;;   "Takes a map of keyword-value pairs or struct and puts a new Entity in the Datastore.
;;   The map or struct must include a :kind String.
;;   Returns the saved Entity converted with entity->map (which will include the assigned :key)."
;;   ([item] (create item nil))
;;   ([item #^Key parent-key]
;;     (let [kind (item :kind)
;;           properties (dissoc (merge {} item) :kind) ; converts struct to map
;;           entity (if parent-key (Entity. kind parent-key) (Entity. kind))]
;;       (doseq [[prop-name value] properties]
;;         (.setProperty entity (name prop-name) value))
;;       (let [key (.put (datastore) entity)]
;;         (assoc (entity->map entity) :key key)))))

;; (defn create
;;   "Takes a map of keyword-value pairs or struct and puts a new Entity in the Datastore.
;;   The map or struct must include a :kind String.
;;   Returns the saved Entity converted with entity->map (which will include the assigned :key)."
;;   ([item] (create item nil))
;;   ([item #^Key parent-key]
;;      (put (assoc item :key (if parent-key (Entity. (:kind item)
;;   parent-key) (Entity. (:kind item)))))))

(defn create [record]
  (put record))

(defmulti update
  "Updates the record with the given properites. The record must be an
  instance of Entity, Key or PersistentHashMap. If one of the attributes'
  values is :remove, then the correponding property name is removed 
  from that entity.  If one of the attributes't values is nil, then the 
  corresponding property name is set to Java's null for that property."
  (fn [record attributes] (class record)))

(defmethod update Entity [#^Entity entity attributes]
  (doseq [[attribute value] attributes]
    (if (not= value :remove)
      (.setProperty entity (name attribute) value)
      (.removeProperty entity (name attribute))))
  (put entity))

(defmethod update Key [#^Key key attributes]
  (update (get key) attributes))

(defmethod update :default [map attributes]
  (update (map->entity map) attributes))

(defmulti delete class)

(defmethod delete Entity [#^Entity entity]
  (delete (.getKey entity)))

(defmethod delete Key [key]
  (.delete (datastore) (into-array [key])))

(defmethod delete clojure.lang.PersistentVector [keys]
  (delete (seq keys)))

(defmethod delete clojure.lang.ISeq [keys]
  (.delete (datastore) (filter-keys keys)))

(defmethod delete :default [map]
  (if-let [key (:key map)]
    (delete key)))



