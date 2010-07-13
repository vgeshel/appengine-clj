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
  (:use [clojure.contrib.string :only (blank? join lower-case replace-str)]
        [clojure.contrib.seq :only (includes?)]
        [appengine.datastore.utils :only (assert-new)]
        appengine.datastore.query
        appengine.datastore.protocols
        appengine.datastore.keys
        appengine.utils
        inflections))

(defn- extract-values [record keys & options]
  (map (fn [[key key-fn]]
         (if-let [value (get record key)]
           (if (fn? key-fn)
             (key-fn value) value)))
       keys))

(defn extract-key
  "Extract the key from the record."
  [record keys & options]
  (let [values (extract-values record keys options)]
    (if (every? (comp not nil?) values)
      (join "-" values))))

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
  (apply vector (seq (extract-option property-specs :key))))

(defn- extract-serializer [property-specs]
  (apply hash-map (flat-seq (extract-option property-specs :serialize))))

(defn- extract-deserializer [property-specs]
  (apply hash-map (flat-seq (merge (extract-option property-specs :serialize)
                                   (extract-option property-specs :deserialize)))))

(defn- humanize [entity]
  (lower-case (replace-str "-" " " (hyphenize (demodulize entity)))))

(defn- entity-fn-doc [entity]
  (str "Make a " (humanize entity) "."))

(defn- entity-fn-sym [entity]
  (symbol (hyphenize (demodulize entity))))

(defn- entity-p-fn-doc [entity]
  (str "Returns true if arg is a " (humanize entity) ", false otherwise."))

(defn- entity-p-fn-sym [entity]
  (symbol (str (hyphenize (demodulize entity)) "?")))

(defn- find-entities-fn-doc [entity]
  (str "Find all " (lower-case (pluralize (stringify entity))) "."))

(defn- find-entities-fn-sym [entity]
  (symbol (str "find-" (hyphenize (pluralize (demodulize entity))))))

(defn- find-entities-by-property-fn-doc [entity property]
  (str "Find all " (lower-case (pluralize (stringify entity))) " by " (stringify property) "."))

(defn- find-entities-by-property-fn-sym [entity property]
  (symbol (str "find-" (hyphenize (pluralize (demodulize entity))) "-by-" (hyphenize (stringify property)))))

(defn- key-fn-doc [entity]
  (str "Make a " (humanize entity) " key."))

(defn- key-fn-sym [entity]
  (symbol (str (entity-fn-sym entity) "-key")))

(defn- key-name-fn-doc [entity]
  (str "Extract the " (humanize entity) " key name."))

(defn- key-name-fn-sym [entity]
  (symbol (str (entity-fn-sym entity) "-key-name")))

(defn entity?
  "Returns true if arg is an Entity, false otherwise."
  [arg] (isa? (class arg) Entity))

(defn entity-kind-name
  "Returns the kind of the entity as a string."
  [record] (if record (pr-str record)))

(defn entity-kind-name
  "Returns the kind of the entity as a string."
  [record] (if record (hyphenize (demodulize record))))

(defn entity-protocol-name
  "Returns the protocol name for an entity. Entity can be a class,
a symbol or a string. If the entity is blank the fn returns nil.

Examples:

  (entity-protocol-name nil)
  ; => nil

  (entity-protocol-name \"\")
  ; => nil

  (entity-protocol-name Continent)
  ; => \"ContinentProtocol\"

  (entity-protocol-name 'Continent)
  ; => \"ContinentProtocol\"

  (entity-protocol-name \"Continent\")
  ; => \"ContinentProtocol\"
"
  [entity] (if-not (blank? (str entity)) (str (demodulize entity) "Protocol")))

;; (entity-protocol-name nil)
;; (entity-protocol-name 'Continent)

(defn make-blank-entity
  "Returns a blank Entity. If called with one parameter, key-or-kind
  must be either a Key or a String, that specifies the kind of the
  entity. If called with multiple parameters, parent must be the Key
  of the parent entity, kind a String that specifies the kind of the
  entity and key-name a String that identifies the entity.

Examples:

  (make-blank-entity \"continent\")
  ; => #<Entity [continent(no-id-yet)]>

  (make-blank-entity (make-key \"continent\" \"eu\"))
  ; => #<Entity [continent(\"eu\")]>
"
  ([key-or-kind]
     (Entity. key-or-kind))
  ([#^Key parent #^String kind key-or-id]
     (Entity. (make-key parent kind key-or-id))))

(defmulti deserialize-entity
  "Convert a Entity into a persistent map. The property values are
stored under their property names converted to a keywords, the
entity's key and kind under the :key and :kind keys.

Examples:

  (deserialize-entity (Entity. \"continent\"))
  ; => {:kind \"continent\", :key #<Key continent(no-id-yet)>}

  (deserialize-entity (doto (Entity. \"continent\")
                     (.setProperty \"name\" \"Europe\")))
  ; => {:name \"Europe\", :kind \"continent\", :key #<Key continent(no-id-yet)>}
"
  (fn [entity] (.getKind entity)))

(defmethod deserialize-entity :default [entity]
  (reduce #(assoc %1 (keyword (key %2)) (val %2))
	  (merge {:kind (.getKind entity) :key (.getKey entity)})
	  (.entrySet (.getProperties entity))))

(defmulti serialize-entity
  "Converts a map into an entity. The kind of the entity is determined
by one of the :key or the :kind keys, which must be in the map.

Examples:

  (serialize-entity {:kind \"person\" :name \"Bob\"})
  ; => #<Entity <Entity [person(no-id-yet)]:
  ;        name = Bob

  (serialize-entity {:key (make-key \"continent\" \"eu\") :name \"Europe\"})
  ; => #<Entity <Entity [continent(\"eu\")]:
  ;        name = Europe
"
  (fn [map]
    (if-let [key (:key map)]
      (.getKind key)
      (:kind map))))

(defmethod serialize-entity :default [map]
  (reduce #(.setProperty %1 (stringify (first %2)) (deserialize (second %2)))
          (Entity. (or (:key map) (:kind map)))
          (dissoc map :key :kind)))

(defn deserialize-property
  "Deserialize the property value with the deserializer."
  [value deserializer]
  (cond (nil? value) nil
        (fn? deserializer) (deserializer value)
        (class? deserializer) (deserialize value)
        :else value))

(defn serialize-property
  "Serialize the property value with the serializer."
  [value serializer]
  (cond (nil? value) nil
        (fn? serializer) (serializer value)
        (class? serializer) (types/serialize serializer value)
        :else value))

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
  (let [deserializers# (extract-deserializer property-specs)
        kind# (entity-kind-name entity)
        key-fns# (extract-key-fns property-specs)
        properties# (map (comp keyword first) property-specs)
        separator# "-"
        serializers# (extract-serializer property-specs)]
    `(do
       
       (defrecord ~entity [~'key ~'kind ~@(map first property-specs)])

       (defn ~(entity-p-fn-sym entity) ~(entity-p-fn-doc entity) [~'arg]
         (cond (isa? (class ~'arg) ~entity) true
               (entity? ~'arg) (= (.getKind ~'arg) ~kind#)
               (map? ~'arg) (= (:kind ~'arg) ~kind#)))

       ;; (defprotocol ~(entity-protocol-name entity))

       (defn ~(key-name-fn-sym entity) ~(key-name-fn-doc entity) [& ~'properties]
         ~(if-not (empty? key-fns#)
            `(cond
              (keyword? (first ~'properties))
              (~(key-name-fn-sym entity) (apply hash-map ~'properties))
              (map? (first ~'properties))
              (let [~'properties (first ~'properties)]
                (extract-key ~'properties ~key-fns#)))))

       (defn ~(key-fn-sym entity) ~(key-fn-doc entity) [~@(if parent '(parent & properties) '(& properties))]
         ~(if-not (empty? key-fns#)
            `(if (map? (first ~'properties))
               (make-key ~(if parent 'parent) ~kind# (~(key-name-fn-sym entity) (first ~'properties)))
               (~(key-fn-sym entity) ~@(if parent '(parent (apply hash-map properties)) '((apply hash-map properties)))))))

       (defn ~(entity-fn-sym entity) ~(entity-fn-doc entity) [~@(if parent '(parent & properties) '(& properties))]
         (let [~'properties (apply hash-map ~'properties)]
           (new ~entity
                (apply ~(key-fn-sym entity) ~@(if parent '(parent (flatten (seq properties))) '((flatten (seq properties)))))
                ~kind#
                ~@(map (fn [key#] `(~key# ~'properties)) properties#))))

       (defn ~(find-entities-fn-sym entity) ~(find-entities-fn-doc entity) [& ~'options]
         (select ~kind#))

       ~@(for [property# properties#]
           `(defn ~(find-entities-by-property-fn-sym entity property#)
              ~(find-entities-by-property-fn-doc entity property#)
              [~'value & ~'options]
              (select ~kind# ~'where (= ~property# (types/serialize ~(property# serializers#) ~'value)))))

       (defmethod ~'deserialize-entity ~kind# [~'entity]
         (new ~entity
              (.getKey ~'entity)
              (.getKind ~'entity)
              ~@(for [property# properties#]
                  `(deserialize-property
                    (.getProperty ~'entity ~(stringify property#))
                    ~(property# deserializers#)))))

       (defmethod ~'serialize-entity ~kind# [~'map]
         (doto (Entity. (or (:key ~'map) (:kind ~'map)))
           ~@(for [property# properties#]
               `(.setProperty
                 ~(stringify property#)
                 (serialize-property (~property# ~'map) ~(property# deserializers#)))))))))

(extend-type Entity
  Record
  (create [entity] (save (assert-new entity)))
  (delete [entity] (delete (.getKey entity)))
  (lookup [entity] (lookup (.getKey entity)))
  (save   [entity] (deserialize-entity (datastore/put entity)))
  (update [entity key-vals] (update (deserialize-entity entity) key-vals))
  Serialization
  (deserialize [entity] (deserialize-entity entity))
  (serialize   [entity] entity))

(extend-type clojure.lang.IPersistentMap
  Record
  (create [map] (create (serialize map)))
  (delete [map] (delete (serialize map)))
  (save   [map] (save (serialize-entity map)))
  (lookup [map] (lookup (serialize-entity map)))
  (update [map key-vals] (save (merge map key-vals)))
  Serialization
  (deserialize [map] map)
  (serialize   [map] (serialize-entity map)))
