(ns #^{:author "Roman Scherer"
       :doc
       "Entity is the fundamental unit of data storage. It has an
immutable identifier (contained in the Key) object, a reference to an
optional parent Entity, a kind (represented as an arbitrary string),
and a set of zero or more typed properties." }
  appengine.datastore.entities
  (:import (com.google.appengine.api.datastore Entity EntityNotFoundException Key))
  (:require [appengine.datastore.service :as datastore]
            [appengine.datastore.types :as types])
  (:use [clojure.contrib.string :only (blank? join lower-case)]
        [clojure.contrib.seq :only (includes?)]
        [appengine.datastore.utils :only (assert-new)]
        appengine.datastore.query
        appengine.datastore.protocols
        appengine.datastore.keys
        appengine.utils
        inflections))

(defn blank-entity
  "Returns a blank Entity. If called with one parameter, key-or-kind
  must be either a Key or a String, that specifies the kind of the
  entity. If called with multiple parameters, parent must be the Key
  of the parent entity, kind a String that specifies the kind of the
  entity and key-name a String that identifies the entity.

Examples:

  (blank-entity \"continent\")
  ; => #<Entity [continent(no-id-yet)]>

  (blank-entity (make-key \"continent\" \"eu\"))
  ; => #<Entity [continent(\"eu\")]>"
  ([key-or-kind]
     (Entity. key-or-kind))
  ([#^Key parent #^String kind #^String key-name]
     (Entity. kind key-name parent)))

(defn entity?
  "Returns true if arg is an Entity, else false."
  [arg] (isa? (class arg) Entity))

(defn entity->map
  "Converts a Entity into a map. The properties of the entity are
  stored under their keyword names, the entity's kind under the :kind
  and the entity's key under the :key key.

Examples:

   (entity->map (Entity. \"continent\"))
   ; => {:kind \"continent\", :key #<Key continent(no-id-yet)>}

   (entity->map (doto (Entity. \"continent\")
                      (.setProperty \"name\" \"Europe\")))
   ; => {:name \"Europe\", :kind \"continent\", :key #<Key continent(no-id-yet)>}"
  [#^Entity entity]
  (reduce #(assoc %1 (keyword (key %2)) (val %2))
	  (merge {:kind (.getKind entity) :key (.getKey entity)})
	  (.entrySet (.getProperties entity))))

(defn entity-kind
  "Returns the kind of the entity as a string."
  [record] (if record (pr-str record)))

(defn- set-property
  "Set the entity's property to value and return the modified entity."
  [entity name value]
  (.setProperty entity (stringify name) (deserialize value))
  entity)

(defn- set-properties
  "Set the entity's properties to the values and return the modified
  entity."
  [entity key-vals]
  (reduce #(set-property %1 (first %2) (second %2)) entity key-vals))

"Converts a map into an entity. The kind of the entity is determined
by one of the :key or the :kind keys, which must be in the map."

(defn map->entity
  "Converts a map into an entity. The kind of the entity is determined
by one of the :key or the :kind keys, which must be in the map.

Examples:

  (map->entity {:kind \"person\" :name \"Bob\"})
  ; => #<Entity <Entity [person(no-id-yet)]:
  ;        name = Bob

  (map->entity {:key (make-key \"continent\" \"eu\") :name \"Europe\"})
  ; => #<Entity <Entity [continent(\"eu\")]:
  ;        name = Europe"
  [map]
  (set-properties
   (Entity. (or (:key map) (:kind map)))
   (dissoc map :key :kind)))

(defn deserialize-fn [& deserializers]
  (let [deserializers (apply hash-map deserializers)]
    (fn [record]
      (if record
        (reduce
         #(let [deserializer (%2 deserializers) value (%2 record)]            
            (assoc %1 %2 
                   (cond
                    (fn? deserializer) (deserializer value)
                    (nil? value) value
                    :else (deserialize value))))
         record (keys record))))))

(defmulti record
  "Returns a blank record for the given key or kind." class)

(defmethod record :default [_] nil)

(defmethod record Key [key]
  (record (.getKind key)))

(defmethod record String [kind-str]
  (if-not (blank? kind-str) (record (symbol kind-str))))

(defmethod record clojure.lang.Symbol [kind-sym]
  (record (resolve kind-sym)))

(defmethod record Class [class]
  (let [blanks (repeat (min-constructor-args class) nil)]
    (eval `(new ~class ~@blanks))))

(defmethod record clojure.lang.IPersistentMap [map]
  (if-let [kind (or (:key map) (:kind map))]
    (if-let [record (record kind)]
      (assoc (merge record map)
        :key (:key map)
        :kind (or (try (.getKind (:key map))) (:kind map))))))

(defn- deserialize-map [map]
  (if-let [record (record map)]
    (deserialize record)
    map))

(defn- serialize-map [map]
  (if-let [record (record map)]
    (serialize record)
    (map->entity map)))

(defn serialize-fn [& serializers]
  (let [serializers (apply hash-map serializers)]
    (fn [record]      
      (if record
        (reduce
         #(let [serialize (%2 serializers) value (%2 record)]
            (.setProperty
             %1 (name %2) 
            (cond
              (fn? serialize) (serialize value)
              (nil? serialize) (types/serialize (class value) value)
              (nil? value) value
              :else (types/serialize serialize value)))
            %1)
         (blank-entity (or (:key record) (:kind record)))
         (keys (dissoc record :key :kind)))))))

(defn- entity?-name [record]
  (symbol (str (hyphenize record) "?")))

(defn- make-entity-name [record]
  (symbol (hyphenize (demodulize record))))

(defn- make-key-name [entity]
  (symbol (str (make-entity-name entity) "-key")))

(defn make-entity-key-fn [parent entity & key-fns]  
  (let [entity-kind (entity-kind entity)
        key-name-fn #(apply key-name (apply hash-map %) key-fns)]
    (if parent
      (fn [parent & properties]
        (if-not (empty? key-fns)
          (make-key parent entity-kind (key-name-fn properties))))
      (fn [& properties]
        (if-not (empty? key-fns)
          (make-key entity-kind (key-name-fn properties)))))))

(defn make-entity-fn [parent entity key-fn & property-keys]
  (let [entity-kind (entity-kind entity)
        record (record entity)
        builder-fn (fn [key properties]
                     (-> record
                         (merge {:key key :kind entity-kind})
                         (merge (select-keys (apply hash-map properties) property-keys))))]
    (if parent
      (fn [parent & properties]
        (builder-fn (apply key-fn parent properties) properties))
      (fn [& properties]
        (builder-fn (apply key-fn properties) properties)))))

(defn- find-entities-name [record]
  (symbol (str "find-" (hyphenize (pluralize (demodulize record))))))

(defn- deserialize-name [record]
  (symbol (str "deserialize-" (hyphenize record))))

(defn- serialize-name [record]
  (symbol (str "serialize-" (hyphenize record))))

(defn- extract-properties [property-specs]
  (reduce
   #(assoc %1 (keyword (first %2)) (apply hash-map (rest %2)))
   (array-map) (reverse property-specs)))

(defn- extract-option [property-specs option]
  (let [properties (extract-properties property-specs)]
    (reduce
     #(if-let [value (option (%2 properties))] (assoc %1 %2 value) %1)
     (array-map) (reverse (keys properties)))))

(defn- extract-key-fns [property-specs]
  (reduce concat (extract-option property-specs :key)))

(defn- extract-serializer [property-specs]
  (flat-seq (extract-option property-specs :serialize)))

(defn- extract-deserializer [property-specs]
  (flat-seq (merge (extract-option property-specs :serialize)
                   (extract-option property-specs :deserialize))))

(defn- extract-meta-data [entity parent property-specs]  
  {:key-fns (extract-key-fns property-specs)
   :kind (entity-kind entity)
   :parent (entity-kind parent)
   :properties (extract-properties property-specs)})

(defmacro defentity
  "A macro to define entitiy records.

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
  [entity [parent] property-specs]
  (let [properties#   (map (comp keyword first) property-specs)
        arglists#     (if parent '(parent & properties) '(& properties))
        params#       (remove #(= % '&) arglists#)
        key-fns#      (extract-key-fns property-specs)
        deserializer# (extract-deserializer property-specs)
        serializer#   (extract-serializer property-specs)
        entity-sym#   (symbol (hyphenize (demodulize entity)))]
    `(do

       (defrecord ~entity [~'key ~'kind ~@(map first property-specs)])

       (defn ~(entity?-name entity)
         ~(str "Returns true if arg is a " entity ", else false.")
         [~'arg] (isa? (class ~'arg) ~entity))
       
       (defn ~(make-key-name entity)
         ~(str "Make a " entity " Key.")
         [~@arglists#] (apply (make-entity-key-fn ~parent ~entity ~@key-fns#) ~@params#))

       (defn ~(make-entity-name entity)
         ~(str "Make a " entity " record.")
         [~@arglists#] (apply (make-entity-fn ~parent ~entity ~(make-key-name entity) ~@properties#) ~@params#))

       ;; (defn ~(find-entities-name entity)
       ;;   ~(str "Find all " entity " records.")
       ;;   [] (select "appengine.datastore.test.entities.Continent"))

       ;; (defn ~(find-entities-name entity)
       ;;   ~(str "Find all " entity " records.")
       ;;   [] (select ~(entity-kind (resolve entity))))

       (extend-type ~entity
         Record
         (~'create [~entity-sym#] (create (serialize ~entity-sym#)))
         (~'delete [~entity-sym#] (delete (serialize ~entity-sym#)))
         (~'save   [~entity-sym#] (save (serialize ~entity-sym#)))
         (~'lookup [~entity-sym#] (lookup (serialize ~entity-sym#)))
         ;; (~'update [~entity-sym# ~'key-vals] (update (serialize ~entity-sym#) ~'key-vals))
         (~'update [~entity-sym# ~'key-vals] (save (merge ~entity-sym# ~'key-vals)))
         Serialization
         (~'deserialize [~entity-sym#] ((deserialize-fn ~@deserializer#) ~entity-sym#))
         (~'serialize [~entity-sym#] ((serialize-fn ~@serializer#) ~entity-sym#))))))

(extend-type Entity
  Record
  (create [entity] (save (assert-new entity)))
  (delete [entity] (delete (.getKey entity)))
  (lookup [entity] (lookup (.getKey entity)))
  (save   [entity] (deserialize (datastore/put entity)))
  (update [entity key-vals] (save (set-properties entity key-vals)))
  Serialization
  (deserialize [entity] (deserialize (entity->map entity)))
  (serialize [entity] entity))

(extend-type clojure.lang.IPersistentMap
  Record
  (create [map] (create (serialize map)))
  (delete [map] (delete (serialize map)))
  (save   [map] (save (serialize map)))
  (lookup [map] (lookup (serialize map)))
  (update [map key-vals] (update (serialize map) key-vals))
  Serialization
  (deserialize [map] (deserialize-map map))
  (serialize [map] (serialize-map map)))
