(ns appengine.datastore.entities
  (:import (com.google.appengine.api.datastore EntityNotFoundException Query Query$FilterOperator))
  (:require [appengine.datastore :as ds])
  (:use inflections))

(defn- find-entities-fn-doc [entity property]
  (str "Find all " (pluralize (str entity)) " by " property "."))

(defn- find-entities-fn-name [entity property]
  (str "find-all-" (pluralize (str entity)) "-by-" property))

(defn- find-entity-fn-doc [entity property]
  (str "Find the first " entity " by " property "."))

(defn- find-entity-fn-name [entity property]
  (str "find-" entity "-by-" property))

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

(defmacro def-property-finder [name doc-string entity [property operator] & [result-fn]]
  (let [property# property]
    `(defn ~(symbol name) ~doc-string
       [~property#]
       (~(or result-fn 'identity)
        (ds/find-all (filter-query '~entity '~property# ~property# ~operator))))))

(defmacro def-finder-fn [entity & properties]
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

(defn compact [seq]
  (remove nil? seq))

(defmacro def-create-fn [parent entity]
  (let [entity# entity parent# parent
        args (compact [parent# 'attributes])]
    `(defn ~(symbol (str "create-" entity#)) [~@args]
       (ds/create (~(symbol (str "make-" entity#)) ~@args)))))

;; (def-create-fn nil continent)
;; (def-create-fn continent country )

(defmacro def-make-fn [parent entity & properties]
  (let [entity# entity parent# parent properties# properties
        args (compact [parent# 'attributes])]
    `(defn ~(symbol (str "make-" entity#)) [~@args]
       (assoc attributes
         :key 1
         :kind ~(str entity#)))))

;; (defn make-key-fn [parent entity & [property]]
;;   (fn [& args]    
;;     (ds/create-key
;;      (if parent (:key (first args)))
;;      (str entity)
;;      (if (property) ((keyword property) entity)))))

;; (defmacro def-make-key-fn [parent entity key]
;;   (let [entity# entity parent# parent properties# properties]
;;     `(defn ~(symbol (str "make-" entity# "-key")) [~@(compact [(if parent# 'parent) 'entity])]
;;        (ds/create-key ~(if parent# '(:key parent)) ~(str entity#)
;;                       (str ~(let [keys (map (fn [property] (list (keyword property) 'entity)) properties#)]
;;                            (if (= (count keys) 1)
;;                              (first keys)
;;                              keys)))))))

;; (def-make-key-fn nil country iso-3166-alpha-2)
;(def-make-key-fn continent country iso-3166-alpha-2)

;; (defn make-country-key [parent properties]
;;   (ds/create-key (:key parent) "country" (:iso-3166-alpha-2 properties)))

;; (defn make-country-key [continent attributes]
;;   (ds/create-key (:key continent) *country-kind* (:iso-3166-alpha-2 attributes)))


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

