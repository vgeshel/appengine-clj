(ns appengine.datastore.entities
  (:import (com.google.appengine.api.datastore 
	    EntityNotFoundException Query Query$FilterOperator))
  (:require [appengine.datastore.core :as ds])
  (:use [clojure.contrib.str-utils2 :only (join)]
        appengine.utils	inflections))

(defn- key-fn-name [entity]
  "Returns the name of the key builder fn for the given entity."
  (str "make-" (str entity) "-key"))

(defmacro def-key-fn [entity entity-keys & [parent]]
  (let [entity# entity entity-keys# entity-keys parent# parent]
    `(defn ~(symbol (key-fn-name entity#)) [~@(compact [parent#]) ~'attributes]
       (ds/create-key
        (:key ~parent#)
        ~(str entity#)
        (join "-" [~@(map #(list % 'attributes) entity-keys#)])))))

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
       (ds/create (~(symbol (str "make-" entity#)) ~@args#)))))

(defn- find-entities-fn-doc [entity property]
  (str "Find all " (pluralize (str entity)) " by " property "."))

(defn- find-entities-fn-name [entity property]
  (str "find-all-" (pluralize (str entity)) "-by-" property))

(defn- find-entity-fn-doc [entity property]
  (str "Find the first " entity " by " property "."))

(defn- find-entity-fn-name [entity property]
  (str "find-" entity "-by-" property))

;; FILTER 

(defn filter-query [entity property value & [operator]]
  (doto (Query. (str entity))
    (.addFilter
     (str property)
     (or operator Query$FilterOperator/EQUAL)
     (if (map? value) ((keyword value) value) value))))

(defn filter-fn [entity property & [operator]]
  (let [operator (or operator Query$FilterOperator/EQUAL)]
    (fn [property-val]
      (ds/find-all
       (filter-query entity property property-val operator)))))

(defmacro deffilter [entity name doc-string [property operator] & [result-fn]]
  (let [property# property]
    `(defn ~(symbol name) ~doc-string
       [~property#]
       (~(or result-fn 'identity)
        ((filter-fn '~entity '~property# ~operator) ~property#)))))

(defmacro def-finder-fn [entity & properties]
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

;; (def-finder-fn country
;;   iso-3166-alpha-2
;;   iso-3166-alpha-3
;;   name)

;; (def-finder-fn country
;;   iso-3166-alpha-2
;;   iso-3166-alpha-3
;;   name)

;; (defentity continent ()  
;;   (iso-3166-alpha-2)
;;   (iso-3166-alpha-3)
;;   (name))

(defmacro def-update-fn [entity]
  (let [entity# entity]
    `(defn ~(symbol (str "update-" entity#)) [~entity# properties]
       (ds/update ~entity properties))))

(defmacro defentity [entity parent & properties]
  (let [entity# entity parent# parent properties# properties]
    `(do
       (def-finder-fn ~entity ~@(map first properties))
       (def-update-fn ~entity))))

;; (def-entity-kind continent)

;; (def-create-fn nil continent)
;; (def-create-fn continent country )

;; (def-make-fn continent)

;; (defn make-key-fn [parent entity & [property]]
;;   (fn [& args]    
;;     (ds/create-key
;;      (if parent (:key (first args)))
;;      (str entity)
;;      (if (property) ((keyword property) entity)))))

;; (defmacro def-key-fn [parent entity key]
;;   (let [entity# entity parent# parent properties# properties]
;;     `(defn ~(symbol (str "make-" entity# "-key")) [~@(compact [(if parent# 'parent) 'entity])]
;;        (ds/create-key ~(if parent# '(:key parent)) ~(str entity#)
;;                       (str ~(let [keys (map (fn [property] (list (keyword property) 'entity)) properties#)]
;;                            (if (= (count keys) 1)
;;                              (first keys)
;;                              keys)))))))

;; (def-key-fn nil country iso-3166-alpha-2)
;(def-key-fn continent country iso-3166-alpha-2)

;; (defn make-country-key [parent properties]
;;   (ds/create-key (:key parent) "country" (:iso-3166-alpha-2 properties)))

;; (defn make-country-key [continent attributes]
;;   (ds/create-key (:key continent) *country-kind* (:iso-3166-alpha-2
;;   attributes)))

;; (def-key-fn continent (:iso-3166-alpha-2 :name))

;; (make-continent-key {:iso-3166-alpha-2 "eu" :name "Europe"})

;; (def-key-fn country (:iso-3166-alpha-2) continent planet)

;; (def-key-fn country (:iso-3166-alpha-2 :name) continent)


;; (make-country-key )

;; (defn make-country [continent attributes]
;;   (assoc attributes
;;     :key (make-country-key continent attributes)
;;     :kind *country-kind*))

;; (defn create-country [continent attributes]
;;   (ds/create (make-country continent attributes)))


;; (defentity continent ()
;;   (name)
;;   (iso-3166-alpha-2 :key true))

;; (defentity country (continent)
;;   (name)
;;   (iso-3166-alpha-2 :key true)
;;   (iso-3166-alpha-3))


;;  ;; DEFINE THE ENTITY KIND

;; (defn- entity-kind-binding-name [entity]
;;   "Returns the name of the entity kind binding."
;;   (str "*" (str entity) "-kind*"))

;; (defmacro def-entity-kind [entity]
;;   "Binds the name of the entity to *entity-kind*."
;;   (let [entity# entity]
;;     `(def ~(symbol (entity-kind-binding-name entity#))
;;           ~(str entity#))))
