(ns appengine.datastore.entities
  (:import (com.google.appengine.api.datastore EntityNotFoundException Query Query$FilterOperator))
  (:require [appengine.datastore :as ds])
  (:use inflections))

(defn- find-entities-fn-doc [entity property]
  (str "Find all " (pluralize (str entity)) " by " property "."))

(defn- find-entities-fn-name [entity property]
  (str "find-" (pluralize (str entity)) "-by-" property))

(defn- find-entity-fn-doc [entity property]
  (str "Find first " entity " by " property "."))

(defn- find-entity-fn-name [entity property]
  (str "find-" entity "-by-" property))

(defn filter-query [entity property value & [operator]]
  (doto (Query. (str entity))
    (.addFilter (str property)
                (or operator Query$FilterOperator/EQUAL)
                (if (map? value) ((keyword value) value) value))))

(defmacro def-property-finder [name doc-string entity [property operator] & [result-fn]]
  (let [property# property]
    `(defn ~(symbol name) ~doc-string
       [~property#]
       (~(or result-fn 'identity)
        (ds/find-all (filter-query '~entity '~property# ~property# ~operator))))))

(defmacro def-finder [entity & properties]
  (let [entity# entity]
    `(do
       ~@(for [property# properties]
           `(do
              (def-property-finder
                ~(find-entities-fn-name entity# property#)
                ~(find-entities-fn-doc entity# property#)
                ~entity# (~property#))
              (def-property-finder
                ~(find-entity-fn-name entity# property#)
                ~(find-entity-fn-doc entity# property#)
                ~entity# (~property#) first))))))

;; (def-finder country
;;   iso-3166-alpha-2
;;   iso-3166-alpha-3
;;   name)

;; (defentity continent ()  
;;   (iso-3166-alpha-2)
;;   (iso-3166-alpha-3)
;;   (name))

;; (defentity country (continent)
;;   (:name :required true)
;;   (:iso-3166-alpha-2 :required true)
;;   (:iso-3166-alpha-3))

