(ns appengine.datastore.entities
  (:import (com.google.appengine.api.datastore 
	    EntityNotFoundException Query Query$FilterOperator))
  (:require [appengine.datastore.core :as ds])
  (:use [clojure.contrib.str-utils2 :only (join)]
        [clojure.contrib.seq-utils :only (includes?)]
        appengine.utils	inflections))

(defn- entity-key? [entity-specs]
  (let [[attribute & options] entity-specs]
    (= (:key (apply hash-map options)) true)))

(defn- entity-keys [entity-specs]
  (map first (filter #(entity-key? %) entity-specs)))

(defn- key-fn-name [entity]
  "Returns the name of the key builder fn for the given entity."
  (str "make-" (str entity) "-key"))

(defmacro def-key-fn [entity entity-keys & [parent]]
  (let [entity# entity entity-keys# entity-keys parent# parent]
    `(defn ~(symbol (key-fn-name entity#)) [~@(compact [parent#]) ~'attributes]
       (ds/create-key
        (:key ~parent#)
        ~(str entity#)
        (join "-" [~@(map #(list (if (keyword? %) % (keyword %)) 'attributes) entity-keys#)])))))

(defmacro def-make-fn [entity & [parent]]
  "Defines a funktion to build entity hashes."
  (let [entity# entity parent# parent
        args# (compact [parent# 'attributes])]
    `(defn ~(symbol (str "make-" entity#)) [~@args#]
       (assoc ~'attributes
         :key (~(symbol (key-fn-name entity)) ~@args#)
         :kind ~(str entity#)))))

(defmacro def-create-fn [entity & [parent]]
  "Defines a funktion to build and save entity hashes."
  (let [entity# entity parent# parent
        args# (compact [parent# 'attributes])]
    `(defn ~(symbol (str "create-" entity#)) [~@args#]
       (ds/create-entity (~(symbol (str "make-" entity#)) ~@args#)))))

(defn- find-entities-fn-doc [entity property]
  (str "Find all " (pluralize (str entity)) " by " property "."))

(defn- find-entities-fn-name [entity property]
  (str "find-all-" (pluralize (str entity)) "-by-" property))

(defn- find-entity-fn-doc [entity property]
  (str "Find the first " entity " by " property "."))

(defn- find-entity-fn-name [entity property]
  (str "find-" entity "-by-" property))

(defn- filter-query [entity property value & [operator]]
  "Returns a query, where the value of the property matches using the
operator."
  (doto (Query. (str entity))
    (.addFilter
     (str property)
     (or operator Query$FilterOperator/EQUAL)
     (if (map? value) ((keyword value) value) value))))

(defn filter-fn [entity property & [operator]]
  "Returns a filter function that returns all entities, where the
property matches the operator."
  (let [operator (or operator Query$FilterOperator/EQUAL)]
    (fn [property-val]
      (ds/find-all
       (filter-query entity property property-val operator)))))

(defmacro deffilter [entity name doc-string [property operator] & [result-fn]]
  "Defines a finder function for the entity."
  (let [property# property]
    `(defn ~(symbol name) ~doc-string
       [~property#]
       (~(or result-fn 'identity)
        ((filter-fn '~entity '~property# ~operator) ~property#)))))

(defmacro def-finder-fn [entity & properties]
  "Defines finder functions the entity."
  (let [entity# entity]
    `(do
       ~@(for [property# properties]
           `(do
              (deffilter ~entity#
                ~(symbol (find-entities-fn-name entity# property#))
                ~(find-entities-fn-doc entity# property#)
                (~property#))
              (deffilter ~entity#
                ~(symbol (find-entity-fn-name entity# property#))
                ~(find-entity-fn-doc entity# property#)
                (~property#) first))))))

(defmacro def-update-fn [entity]
  "Defines an update function for the entity."
  (let [entity# entity]
    `(defn ~(symbol (str "update-" entity#)) [~entity# & ~'properties]
       (ds/update-entity ~entity (apply hash-map ~'properties)))))

(defmacro defentity [entity parent & properties]
  (let [entity# entity parent# parent properties# properties]
    `(do
       (def-key-fn ~entity# ~(entity-keys properties) ~@parent#)
       (def-make-fn ~entity# ~@parent#)
       (def-create-fn ~entity# ~@parent#)
       (def-finder-fn ~entity# ~@(map first properties#))
       (def-update-fn ~entity#)
       )))
