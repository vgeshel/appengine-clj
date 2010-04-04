(ns appengine.datastore.test.entities
  (:require [appengine.datastore :as ds])
  (:use appengine.datastore.entities
        appengine.test-utils
        clojure.test))

(refer-private 'appengine.datastore.entities)

(defn create-country [name & [iso-3166-alpha-2]]
  (ds/create {:kind "country" :name name :iso-3166-alpha-2 iso-3166-alpha-2}))

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
  (let [country (create-country "Spain")]
    (is (= [country] (ds/find-all (filter-query 'country 'name "Spain"))))
    (is (= [country] (ds/find-all (filter-query "country" "name" "Spain"))))))

(dstest test-filter-fn
  (is (fn? (filter-fn 'country 'name)))
  (let [country (create-country "Spain")]
    (is (= [country] ((filter-fn 'country 'name) "Spain")))
    (is (= [country] ((filter-fn "country" "name") "Spain")))))

(dstest test-find-all-countries-by-name
  (deffilter find-all-countries-by-name
    "Find all countries by name."
    country (name))
  (let [country (create-country "Spain")]
    (is (= [country] (find-all-countries-by-name (:name country))))))

(dstest test-find-country-by-name
  (deffilter find-country-by-name
    "Find all countries by name."
    country (name) first)
  (let [country (create-country "Spain")]
    (is (= country (find-country-by-name (:name country))))))

(dstest test-def-finder-fn
  (def-finder-fn country name iso-3166-alpha-2)
  (let [country (create-country "Spain" "es")]
    (is (= country (find-country-by-name (:name country))))
    (is (= country (find-country-by-iso-3166-alpha-2 (:iso-3166-alpha-2 country))))    
    (is (= [country] (find-all-countries-by-name (:name country))))
    (is (= [country] (find-all-countries-by-iso-3166-alpha-2 (:iso-3166-alpha-2 country))))))

(dstest test-property-finder
  (deffilter country find-all-countries-by-name
    "Find all countries by name." (name))
  (is (= (:doc (meta ('find-all-countries-by-name (ns-interns *ns*))))
         "Find all countries by name."))
  (is (fn? find-all-countries-by-name))
  (let [country (create-country "Spain" "es")]
    (is (= [country] (find-all-countries-by-name (:name country))))))

;; (dstest test-def-make-key-fn-without-parent
;;   (def-make-key-fn nil continent iso-3166-alpha-2)
;;   (let [key (make-continent-key {:iso-3166-alpha-2 "eu"})]
;;     (is (= (.getParent key) nil))
;;     (is (= (.getKind key) "continent"))
;;     (is (= (.getName key) "eu"))
;;     (is (= (.getId key) 0))
;;     (is (.isComplete key))))

;; (dstest test-def-make-key-fn-with-parent
;;   (def-make-key-fn continent country iso-3166-alpha-2)
;;   (let [continent {:key (ds/create-key "continent" "eu")}
;;         key (make-country-key continent {:iso-3166-alpha-2 "de"})]
;;     (is (= (.getParent key) (:key continent)))
;;     (is (= (.getKind key) "country"))
;;     (is (= (.getName key) "de"))
;;     (is (= (.getId key) 0))
;;     (is (.isComplete key))))

;; (dstest test-def-make-key-fn-with-compound-key
;; ;  (def-make-key-fn continent country iso-3166-alpha-2 name)
;;   (let [continent {:key (ds/create-key "continent" "eu")}
;;         key (make-country-key continent {:iso-3166-alpha-2 "de" :name "Germany"})]
;;     (println key)
;;     ;; (is (= (.getParent key) (:key continent)))
;;     ;; (is (= (.getKind key) "country"))
;;     ;; (is (= (.getName key) "de"))
;;     ;; (is (= (.getId key) 0))
;;     (is (.isComplete key))))

;; (dstest test-filter-fn
;;   (let [country (create-country "Spain")]
;;     (is (= [country] ((filter-fn "country" "name" country))))    
;;     (is (= [country] ((filter-fn "country" "name" (:name country)))))))

;; (def-finder-fn country
;;   iso-3166-alpha-2
;;   iso-3166-alpha-3
;;   name)
