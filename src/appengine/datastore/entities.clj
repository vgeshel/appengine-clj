(ns #^{:author "Roman Scherer"
       :doc "The entity API for the Google App Engine datastore service." }
  appengine.datastore.entities
  (:import (com.google.appengine.api.datastore 
	    Entity EntityNotFoundException Key Query Query$FilterOperator
            GeoPt
            ))
  (:require [appengine.datastore.core :as ds]
            [appengine.datastore.types :as types])
  (:use [clojure.contrib.string :only (join lower-case)]
        [clojure.contrib.seq :only (includes?)]
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

  (blank-entity (create-key \"continent\" \"eu\"))
  ; => #<Entity [continent(\"eu\")]>"
  ([key-or-kind]
     (Entity. key-or-kind))
  ([#^Key parent #^String kind #^String key-name]
     (Entity. kind key-name parent)))

(defn- blank-record [record n-fields]
  (eval `(new ~record ~@(take n-fields (repeat nil)))))

(defn property-class
  "Returns the class of the record's property."
  [record property]  
  (or (:type (property (:properties (meta record))))
      (class (property record))))

(defn serialze-property-fn
  "Returns a function to serialize the record's property."
  [record property]  
  (or (:serialize (property (:properties (meta record))))
      (fn [value]
        (types/serialize (property-class record property) value))))

(defn deserialze-property-fn
  "Returns a function to deserialize the record's property."
  [record property]  
  (or (:deserialize (property (:properties (meta record))))
      (fn [value]
        (types/deserialize value))))

(defn serialize-entity [record]
  
  )

(defn- entity?-sym [record]
  (symbol (str (hyphenize record) "?")))

(defn- entity?-doc [record]
  (str "Returns true if arg is a " record ", else false."))

(defn- make-entity-sym [record]
  (symbol (str "make-" (hyphenize record))))

(defn- make-entity-doc [entity]
  (str "Returns a new " entity " record."))

(defn- make-entity-key-doc [entity]
  (str "Returns a new " entity " key."))

(defn- make-entity-key-sym [entity]
  (symbol (str (make-entity-sym entity) "-key")))

(defn make-entity-key-fn [parent entity & key-fns]  
  (let [entity-kind (hyphenize entity)
        key-name-fn #(apply key-name (apply hash-map %) key-fns)]
    (if parent
      (fn [parent & key-vals]
        (if-not (empty? key-fns)
          (create-key parent entity-kind (key-name-fn key-vals))))
      (fn [& key-vals]
        (if-not (empty? key-fns)
          (create-key entity-kind (key-name-fn key-vals)))))))

(defn make-entity-fn [parent entity & property-keys]  
  (let [entity-kind (hyphenize entity)
        record (blank-record entity (+ 2 (count property-keys)))
        key-fn (resolve (make-entity-key-sym entity))
        builder-fn (fn [key properties]
                     (-> record
                         (merge {:key key :kind entity-kind})
                         (merge (select-keys (apply hash-map properties) property-keys))))]
    (if parent
      (fn [parent & properties]
        (builder-fn (apply key-fn parent properties) properties))
      (fn [& properties]        
        (builder-fn (apply key-fn properties) properties)))))

(defn- extract-properties [specifications]
  (zipmap
   (map #(keyword (first %)) specifications)
   (map #(apply hash-map (rest %)) specifications)))

(defn- extract-key-fns [specifications]
  (let [properties (extract-properties specifications)]
    (apply vector (map #(vector % (:key (% properties)))
                       (remove #(nil? (:key (% properties)))
                               (map (comp keyword first) specifications))))))

(defn- make-meta-data [entity parent specifications]  
  {:key-fns (extract-key-fns specifications)
   :kind (hyphenize entity)
   :parent (if parent (hyphenize parent))
   :properties (extract-properties specifications)})

(defmacro defentity
  "A macro to define entitiy records.

Examples:

  (defentity Continent ()
    (iso-3166-alpha-2 :key lower-case)
    (location :type GeoPt)
    (name))

  (defentity Country (Continent)
    (iso-3166-alpha-2 :key lower-case)
    (location :type GeoPt)
    (name))

"
  [entity [parent] & specifications]
  (let [entity# entity
        parent# parent
        meta-data# (make-meta-data entity# parent# specifications)
        properties# (map (comp keyword first) specifications)
        arglists# (if parent# '['parent '& 'properties] '['& 'properties])
        ]
    `(do

       (defrecord ~entity# [~'key ~'kind ~@(map first specifications)])

       (defn ~(entity?-sym entity#)
         ~(entity?-doc entity#)
         [~'arg]
         (isa? (class ~'arg) ~entity#))

       (def ~(with-meta (make-entity-key-sym entity#)
               {:arglists arglists# :doc (make-entity-key-doc entity#)})
            (make-entity-key-fn '~parent# '~entity# ~@(flatten (:key-fns meta-data#))))

       (def ~(with-meta (make-entity-sym entity#)
               {:arglists arglists# :doc (make-entity-doc entity#)})
            (make-entity-fn '~parent# '~entity# ~@properties#))       

       ;; (def ~(make-entity-sym entity#)
       ;;      (make-entity-fn '~parent# '~entity# ~@(flatten (:key-fns meta-data#))))

       ;; (defn ~(make-entity-sym entity#) [~@(compact [parent# '& 'properties])]
       ;;   (let [~'properties (apply hash-map ~'properties)]
       ;;     (with-meta
       ;;       (new ~entity#
       ;;            (or (:key ~'properties)
       ;;                (apply ~(make-entity-key-sym entity#)
       ;;                       ~@(compact [parent# '(flatten (seq properties))])))
       ;;            ~(hyphenize entity#)                 
       ;;            ~@(map (fn [property] (list property 'properties)) properties#))
       ;;       ~meta-data#)))
       
       )))

;; (defn make-entity-fn [parent entity & properties]  
;;   (let [entity-kind (hyphenize entity)
;;         key-name-fn #(apply key-name (apply hash-map %) key-fns)]
;;     (if parent
;;       (fn [parent & properties]
;;         (if-not (empty? key-fns)
;;           (create-key parent entity-kind (key-name-fn properties))))
;;       (fn [& properties]
;;         (if-not (empty? key-fns)
;;           (create-key entity-kind (key-name-fn properties)))))))

;; (with-local-datastore
;;   ((make-entity-key-fn 'Continent 'Country :iso-3166-alpha-2 #'lower-case)
;;    (create-key "continent" "eu") :iso-3166-alpha-2 "DE" :name "Germany"))

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
;;           `(ds/create-key
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
