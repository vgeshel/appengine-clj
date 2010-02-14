(ns appengine.datastore.test.entities
  (:require [appengine.datastore :as ds])
  (:use appengine.datastore.entities
        appengine.test-utils
        clojure.test))

(defn create-country [name & [iso-3166-alpha-2]]
  (ds/create {:kind "country" :name name :iso-3166-alpha-2 iso-3166-alpha-2}))

(dstest test-filter-query
  (let [country (create-country "Spain")]
    (is (= [country] (ds/find-all (filter-query 'country 'name "Spain"))))
    (is (= [country] (ds/find-all (filter-query "country" "name" "Spain"))))))

(dstest test-find-countries-by-name
  (def-property-finder find-countries-by-name
    "Find all countries by name."
    country (name))
  (let [country (create-country "Spain")]
    (is (= [country] (find-countries-by-name (:name country))))))

(dstest test-find-country-by-name
  (def-property-finder find-country-by-name
    "Find all countries by name."
     country (name) first)
  (let [country (create-country "Spain")]
    (is (= country (find-country-by-name (:name country))))))

(dstest test-def-finder
  (def-finder country name iso-3166-alpha-2)
  (let [country (create-country "Spain" "es")]
    (is (= country (find-country-by-name (:name country))))
    (is (= country (find-country-by-iso-3166-alpha-2 (:iso-3166-alpha-2 country))))    
    (is (= [country] (find-countries-by-name (:name country))))
    (is (= [country] (find-countries-by-iso-3166-alpha-2 (:iso-3166-alpha-2 country))))))

;; (dstest test-filter-fn
;;   (let [country (create-country "Spain")]
;;     (is (= [country] ((filter-fn "country" "name" country))))    
;;     (is (= [country] ((filter-fn "country" "name" (:name country)))))))

;; (def-finder country
;;   iso-3166-alpha-2
;;   iso-3166-alpha-3
;;   name)
