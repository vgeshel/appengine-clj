(ns #^{:author "Roman Scherer"
       :doc
       "Entity is the fundamental unit of data storage. It has an
immutable identifier (contained in the Key) object, a reference to an
optional parent Entity, a kind (represented as an arbitrary string),
and a set of zero or more typed properties." }
appengine.datastore.entities
  (:import (com.google.appengine.api.datastore 
	    Entity EntityNotFoundException Key Query Query$FilterOperator
            GeoPt
            ))
  (:require [appengine.datastore.types :as types]
            [appengine.datastore.service :as datastore])
  (:use [clojure.contrib.string :only (blank? join lower-case)]
        [clojure.contrib.seq :only (includes?)]
        [appengine.datastore.utils :only (assert-new)]
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

(defn- blank-record [record]
  (let [record (if (symbol? record) (resolve record) record)
        number-of-args (apply min (map count (map #(.getParameterTypes %) (.getConstructors record))))]
    (eval `(new ~record ~@(repeat number-of-args nil)))))

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
  "Returns the ind of the entity as a string."
  [record] (if record (pr-str record)))

(defn- set-property
  "Set the entity's property to value and return the modified entity."
  [entity name value]
  (.setProperty entity (stringify name) (types/deserialize value))
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

(defn deserialize-fn [record & deserializers]
  (let [record (blank-record record)
        deserializers (apply hash-map deserializers)]
    (fn [entity]
      (if entity
        (let [entries (.entrySet (.getProperties entity))]
          (reduce
           #(assoc %1 (keyword (key %2)) (types/deserialize (val %2)))
           (assoc record :key (.getKey entity) :kind (.getKind entity))
           entries))))))

(defn deserialize-fn [& deserializers]
  (let [deserializers (apply hash-map deserializers)]
    (fn [record]
      (if record
        (reduce
         #(let [deserialize (%2 deserializers) value (%2 record)]            
            (assoc %1 %2 
                   (cond
                    (fn? deserialize) (deserialize value)
                    (nil? value) value
                    :else (types/deserialize value))))
         record (keys record))))))

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
        record (blank-record entity)
        builder-fn (fn [key properties]
                     (-> record
                         (merge {:key key :kind entity-kind})
                         (merge (select-keys (apply hash-map properties) property-keys))))]
    (if parent
      (fn [parent & properties]
        (builder-fn (apply key-fn parent properties) properties))
      (fn [& properties]
        (builder-fn (apply key-fn properties) properties)))))

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
        entity-sym#   (symbol (hyphenize (demodulize entity)))
        ns# *ns*
        ]
    `(do

       (defrecord ~entity [~'key ~'kind ~@(map first property-specs)])

       (defn ~(entity?-name entity)
         ~(str "Returns true if arg is a " entity ", else false.")
         [~'arg] (isa? (class ~'arg) ~entity))

       (let [key-fn# (make-entity-key-fn ~parent ~entity ~@key-fns#)
             entity-fn# (make-entity-fn ~parent ~entity key-fn# ~@properties#)]

         (defn ~(make-key-name entity)
           ~(str "Make a " entity " Key.")
           [~@arglists#] (apply key-fn# ~@params#))

         (defn ~(make-entity-name entity)
           ~(str "Make a " entity " record.")
           [~@arglists#]
           ;; (apply (make-entity-fn ~parent ~entity ~@properties#) ~@params#)
           (apply entity-fn# ~@params#)
           ))

       
       (extend-type ~entity
         Datastore
         (~'create [~entity-sym#] (create (serialize ~entity-sym#)))
         (~'delete [~entity-sym#] (delete (serialize ~entity-sym#)))
         (~'save   [~entity-sym#] (save (serialize ~entity-sym#)))
         (~'select [~entity-sym#] (select (serialize ~entity-sym#)))
         (~'update [~entity-sym# ~'key-vals] (update (serialize ~entity-sym#) ~'key-vals))
         Serialization
         (~'deserialize [~entity-sym#] ((deserialize-fn ~@deserializer#) ~entity-sym#))
         (~'serialize [~entity-sym#] ((serialize-fn ~@serializer#) ~entity-sym#)))

       
       ;; (in-ns 'appengine.datastore.types)
       ;; (defmethod ~'deserialize ~(entity-kind (resolve entity)) [~'entity]
       ;;            ((deserialize-fn ~(resolve entity) ~@deserializer#) ~'entity))
       ;; (in-ns '~(symbol (str ns#)))
         
       )))

(extend-type Entity
  Datastore
  (create [entity] (save (assert-new entity)))
  (delete [entity] (delete (.getKey entity)))
  (select [entity] (select (.getKey entity)))
  (save   [entity] (deserialize (datastore/put entity)))
  (update [entity key-vals] (save (set-properties entity key-vals)))
  Serialization
  (deserialize [entity] (deserialize (entity->map entity)))
  (serialize [entity] entity))

(extend-type clojure.lang.IPersistentMap
  Datastore
  (create [map] (create (serialize map)))
  (delete [map] (delete (serialize map)))
  (save   [map] (save (serialize map)))
  (select [map] (select (serialize map)))
  (update [map key-vals] (update (serialize map) key-vals))
  Serialization
  (deserialize [map]
               (if-let [record (resolve (symbol (:kind map)))]
                 (deserialize (merge (blank-record record) map))
                 map))
  (serialize [map] ((serialize-fn) map)))

;; (use 'appengine.test)

;; (with-local-datastore
;;   (serialize {:kind "continent"}))

;; (with-local-datastore
;;   (serialize (array-map :kind "continent")))

;; (with-local-datastore
;;   ((make-entity-key-fn 'Continent 'Country :iso-3166-alpha-2 #'lower-case)
;;    (make-key "continent" "eu") :iso-3166-alpha-2 "DE" :name "Germany"))

;; (with-local-datastore
;;   ((make-entity-key-fn nil 'Continent :iso-3166-alpha-2 #'lower-case :name #'lower-case)
;;    :iso-3166-alpha-2 "EU" :name "Europe"))

;; (defentity Continent ()
;;   (iso-3166-alpha-2 :key lower-case)
;;   (location :type GeoPt)
;;   (name))

;; (defentity Country (Continent)
;;   (iso-3166-alpha-2 :key true)
;;   (location :type GeoPt)
;;   (name))

;; (make-continent :name "Europe")
;; (meta (make-continent :name "Europe"))
;; assoc

;; (defn- set-property [entity record property]
;;   (let [value ((serialze-property-fn record property) (property record))]
;;     (.setProperty entity (str property) value)
;;     entity))

;; (defprotocol Serialize
;;   (serialize [entity] "Serialize the entity."))

;; (defprotocol Deserialize
;;   (deserialize [entity] "Deserialize the entity."))

;; (extend-type clojure.lang.PersistentArrayMap
;;   Serialize
;;   (serialize [map]
;;     (let [entity (blank-entity (or (:key map) (:kind map)))]
;;       (set-properties entity map))))

;; (class {})

;; (serialize {})

;; (defn- entity-key? [entity-specs]
;;   (let [[attribute & options] entity-specs]
;;     (= (:key (apply hash-map options)) true)))

;; (defn- entity-keys [entity-specs]
;;   (map first (filter #(entity-key? %) entity-specs)))

;; (defn- find-entities-fn-doc [entity property]
;;   (str "Find all " (pluralize (str entity)) " by " property "."))

;; (defn- find-entities-fn-name [entity property]
;;   (str "find-all-" (pluralize (str entity)) "-by-" property))

;; (defn- find-entity-fn-doc [entity property]
;;   (str "Find the first " entity " by " property "."))

;; (defn- find-entity-fn-name [entity property]
;;   (str "find-" entity "-by-" property))

;; (defn- filter-query [entity property value & [operator]]
;;   "Returns a query, where the value of the property matches using the
;; operator."
;;   (doto (Query. (str entity))
;;     (.addFilter
;;      (str property)
;;      (or operator Query$FilterOperator/EQUAL)
;;      (if (map? value) ((keyword value) value) value))))

;; (defn filter-fn [entity property & [operator]]
;;   "Returns a filter function that returns all entities, where the
;; property matches the operator."
;;   (let [operator (or operator Query$FilterOperator/EQUAL)]
;;     (fn [property-val]
;;       (ds/find-all
;;        (filter-query entity property property-val operator)))))

;; (defn- key-fn-name [entity]
;;   "Returns the name of the key builder fn for the given entity."
;;   (str "make-" (str entity) "-key"))

;; (defmacro def-key-fn [entity entity-keys & [parent]]
;;   "Defines a function to build a key from the entity-keys
;; propertoes. If entity-keys is empty the fn returns nil."
;;   (let [entity# entity entity-keys# entity-keys parent# parent]
;;     `(defn ~(symbol (key-fn-name entity#)) [~@(compact [parent#]) ~'attributes]
;;        ~(if-not (empty? entity-keys#)
;;           `(ds/make-key
;;             (:key ~parent#)
;;             ~(str entity#)
;;             (join "-" [~@(map #(list (if (keyword? %) % (keyword %)) 'attributes) entity-keys#)]))))))

;; (defmacro def-make-fn [entity parent & properties]
;;   "Defines a function to build entity hashes."
;;   (let [entity# entity parent# (first parent)
;;         properties# properties
;;         args# (compact [parent# 'attributes])]
;;     `(defn ~(symbol (str "make-" entity#)) [~@args#]       
;;        (merge (assoc (select-keys ~'attributes [~@(map (comp keyword first) properties#)])
;;                 :kind ~(str entity#))
;; 	      (let [key# (~(symbol (key-fn-name entity)) ~@args#)]
;; 		(if-not (nil? key#) {:key key#} {}))))))

;; (defmacro def-create-fn [entity & [parent]]
;;   "Defines a function to build and save entity hashes."
;;   (let [entity# entity parent# parent
;;         args# (compact [parent# 'attributes])]
;;     `(defn ~(symbol (str "create-" entity#)) [~@args#]
;;        (ds/create-entity (~(symbol (str "make-" entity#)) ~@args#)))))

;; (defmacro deffilter [entity name doc-string [property operator] & [result-fn]]
;;   "Defines a finder function for the entity."
;;   (let [property# property]
;;     `(defn ~(symbol name) ~doc-string
;;        [~property#]
;;        (~(or result-fn 'identity)
;;         ((filter-fn '~entity '~property# ~operator) ~property#)))))

;; (defmacro def-find-all-by-property-fns [entity & properties]
;;   "Defines a function for each property, that find all entities by a property."
;;   (let [entity# entity]
;;     `(do
;;        ~@(for [property# properties]
;;            `(do
;;               (deffilter ~entity#
;;                 ~(symbol (find-entities-fn-name entity# property#))
;;                 ~(find-entities-fn-doc entity# property#)
;;                 (~property#)))))))

;; (defmacro def-find-first-by-property-fns [entity & properties]
;;   "Defines a function for each property, that finds the first entitiy by a property."
;;   (let [entity# entity]
;;     `(do
;;        ~@(for [property# properties]
;;            `(do
;;               (deffilter ~entity#
;;                 ~(symbol (find-entity-fn-name entity# property#))
;;                 ~(find-entity-fn-doc entity# property#)
;;                 (~property#) first))))))

;; (defmacro def-delete-fn [entity]
;;   "Defines a delete function for the entity."
;;   (let [entity# entity]
;;     `(defn ~(symbol (str "delete-" entity#)) [& ~'args]
;;        (ds/delete-entity ~'args))))

;; (defmacro def-find-all-fn [entity]
;;   "Defines a function that returns all entities."
;;   (let [entity# entity]
;;     `(defn ~(symbol (str "find-" (pluralize (str entity#)))) []
;;        (ds/find-all (Query. ~(str entity#))))))

;; (defmacro def-update-fn [entity]
;;   "Defines an update function for the entity."
;;   (let [entity# entity]
;;     `(defn ~(symbol (str "update-" entity#)) [~entity# ~'properties]
;;        (ds/update-entity ~entity ~'properties))))

;; (defmacro defentity [entity parent & properties]
;;   "Defines helper functions for the entity. Note that
;;    if no property is qualified by :key true, then the data
;;    store will create a unique key for this object.  However
;;    note that the result of calling make-*entity*-key for any 
;;    such object is nil and not a proper key."
;;   (let [entity# entity parent# parent properties# properties]
;;     `(do
;;        (def-key-fn ~entity# ~(entity-keys properties) ~@parent#)
;;        (def-make-fn ~entity# ~parent# ~@properties#)
;;        (def-create-fn ~entity# ~@parent#)
;;        (def-delete-fn ~entity#)
;;        (def-find-all-by-property-fns ~entity# ~@(map first properties#))
;;        (def-find-all-fn ~entity#)
;;        (def-find-first-by-property-fns ~entity# ~@(map first properties#))
;;        (def-update-fn ~entity#))))
