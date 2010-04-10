(ns appengine.datastore.test.entities
  (:require [appengine.datastore.core :as ds])
  (:use appengine.datastore.entities
        appengine.test-utils
        clojure.test))

(refer-private 'appengine.datastore.entities)

(deftest test-find-entities-fn-doc
  (is (= (find-entities-fn-doc 'country 'iso-3166-alpha-2)
         "Find all countries by iso-3166-alpha-2."))
  (is (= (find-entities-fn-doc 'sheep 'color)
         "Find all sheep by color.")))

(deftest test-find-entities-fn-name
  (is (= (find-entities-fn-name 'country 'iso-3166-alpha-2)
         "find-all-countries-by-iso-3166-alpha-2"))
  (is (= (find-entities-fn-name 'sheep 'color)
         "find-all-sheep-by-color")))

(deftest test-find-entity-fn-doc
  (is (= (find-entity-fn-doc 'country 'iso-3166-alpha-2)
         "Find the first country by iso-3166-alpha-2."))
  (is (= (find-entity-fn-doc 'sheep 'color)
         "Find the first sheep by color.")))

(deftest test-find-entity-fn-name
  (is (= (find-entity-fn-name 'country 'iso-3166-alpha-2)
         "find-country-by-iso-3166-alpha-2"))
  (is (= (find-entity-fn-name 'sheep 'color)
         "find-sheep-by-color")))

(dstest test-filter-query
  (let [continent (ds/create {:kind "continent" :name "Europe"})]
    (is (= [continent] (ds/find-all (filter-query 'continent 'name "Europe"))))
    (is (= [continent] (ds/find-all (filter-query "continent" "name" "Europe"))))))

(dstest test-filter-fn
  (is (fn? (filter-fn 'continent 'name)))
  (let [continent (ds/create {:kind "continent" :name "Europe"})]
    (is (= [continent] ((filter-fn 'continent 'name) "Europe")))
    (is (= [continent] ((filter-fn "continent" "name") "Europe")))))

(dstest test-find-all-continents-by-name
  (deffilter continent find-all-continents-by-name
    "Find all continents by name."
    (name))
  (let [continent (ds/create {:kind "continent" :name "Europe"})]
    (is (= [continent] (find-all-continents-by-name (:name continent))))))

(dstest test-find-continent-by-name
  (deffilter continent find-continent-by-name
    "Find all countries by name." (name) first)
  (let [continent (ds/create {:kind "continent" :name "Europe"})]
    (is (= continent (find-continent-by-name (:name continent))))))

(dstest test-def-finder-fn
  (def-finder-fn continent name iso-3166-alpha-2)
  (let [continent (ds/create {:kind "continent" :name "Europe" :iso-3166-alpha-2 "eu"})]
    (is (= continent (find-continent-by-name (:name continent))))
    (is (= continent (find-continent-by-iso-3166-alpha-2 (:iso-3166-alpha-2 continent))))    
    (is (= [continent] (find-all-continents-by-name (:name continent))))
    (is (= [continent] (find-all-continents-by-iso-3166-alpha-2 (:iso-3166-alpha-2 continent))))))

(deftest test-key-fn-name
  (is (= (key-fn-name 'continent) "make-continent-key")))

(dstest test-make-continent-key
  (def-key-fn continent (:iso-3166-alpha-2))
  (let [key (make-continent-key {:iso-3166-alpha-2 "eu"})]
    (is (= (class key) com.google.appengine.api.datastore.Key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))    
    (is (= (.getKind key) "continent"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "eu"))))

(dstest test-make-country-key
  (def-key-fn country (:iso-3166-alpha-2) continent)
  (let [continent {:key (ds/create-key "continent" "eu")}
        key (make-country-key continent {:iso-3166-alpha-2 "es"})]
    (is (= (class key) com.google.appengine.api.datastore.Key))
    (is (.isComplete key))
    (is (= (.getParent key) (:key continent)))    
    (is (= (.getKind key) "country"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "es"))))

(dstest test-def-create-fn-with-continent
  (def-key-fn continent (:iso-3166-alpha-2))
  (def-make-fn continent)
  (def-create-fn continent)
  (let [continent (create-continent {:iso-3166-alpha-2 "eu" :name "Europe"})]
    (let [key (:key continent)]
      (is (= (class key) com.google.appengine.api.datastore.Key))
      (is (.isComplete key))
      (is (nil? (.getParent key)))    
      (is (= (.getKind key) "continent"))
      (is (= (.getId key) 0))
      (is (= (.getName key) "eu")))
    (is (= (:kind continent) "continent"))
    (is (= (:iso-3166-alpha-2 continent) "eu"))
    (is (= (:name continent) "Europe"))))

(dstest test-def-create-fn-with-create-country
  (def-key-fn continent (:iso-3166-alpha-2))
  (def-make-fn continent)
  (def-create-fn continent)
  (def-key-fn country (:iso-3166-alpha-2) continent)
  (def-make-fn country continent)
  (def-create-fn country continent)
    (let [continent (create-continent {:iso-3166-alpha-2 "eu" :name "Europe"})
          country (create-country continent {:iso-3166-alpha-2 "es" :name "Spain"})]
    (let [key (:key country)]
      (is (= (class key) com.google.appengine.api.datastore.Key))
      (is (.isComplete key))
      (is (= (.getParent key) (:key continent)))    
      (is (= (.getKind key) "country"))
      (is (= (.getId key) 0))
      (is (= (.getName key) "es")))
    (is (= (:kind country) "country"))
    (is (= (:iso-3166-alpha-2 country) "es"))
    (is (= (:name country) "Spain"))))

(dstest test-def-make-fn-continent-with-continent
  (def-make-fn continent)
  (let [continent (make-continent {:iso-3166-alpha-2 "eu" :name "Europe"})]
    (let [key (:key continent)]
      (is (= (class key) com.google.appengine.api.datastore.Key))
      (is (.isComplete key))
      (is (nil? (.getParent key)))    
      (is (= (.getKind key) "continent"))
      (is (= (.getId key) 0))
      (is (= (.getName key) "eu")))
    (is (= (:kind continent) "continent"))
    (is (= (:iso-3166-alpha-2 continent) "eu"))
    (is (= (:name continent) "Europe"))))

(dstest test-def-make-fn-with-country
  (def-make-fn continent)
  (def-make-fn country continent)
  (let [continent (make-continent {:iso-3166-alpha-2 "eu" :name "Europe"})
        country (make-country continent {:iso-3166-alpha-2 "es" :name "Spain"})]
    (let [key (:key country)]
      (is (= (class key) com.google.appengine.api.datastore.Key))
      (is (.isComplete key))
      (is (= (.getParent key) (:key continent)))    
      (is (= (.getKind key) "country"))
      (is (= (.getId key) 0))
      (is (= (.getName key) "es")))
    (is (= (:kind country) "country"))
    (is (= (:iso-3166-alpha-2 country) "es"))
    (is (= (:name country) "Spain"))))

(dstest test-property-finder
  (deffilter continent find-all-continents-by-name
    "Find all continents by name." (name))
  (is (fn? find-all-continents-by-name))
  (let [continent (ds/create {:kind "continent" :name "Europe"})]
    (is (= [continent] (find-all-continents-by-name (:name continent))))))
