(ns appengine.datastore.test.entities
  (:require [appengine.datastore.core :as ds])
  (:use appengine.datastore.entities
        appengine.test-utils
        clojure.test))

(refer-private 'appengine.datastore.entities)

(defentity continent ()
  (iso-3166-alpha-2 :key true)
  (name))

(defentity country (continent)
  (iso-3166-alpha-2 :key true)
  (iso-3166-alpha-3)
  (name))

(defentity region (country)
  (code :key true)
  (name))

(deftest test-entity-key?
  (is (entity-key? '(iso-3166-alpha-2 :key true)))
  (is (not (entity-key? '(iso-3166-alpha-2))))
  (is (not (entity-key? '(iso-3166-alpha-2 :key false)))))

(deftest test-entity-keys
  (is (= (entity-keys '((iso-3166-alpha-2 :key true) (name :key false)))
         ['iso-3166-alpha-2])))

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
  (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
    (is (= [continent] (ds/find-all (filter-query 'continent 'name "Europe"))))
    (is (= [continent] (ds/find-all (filter-query "continent" "name" "Europe"))))))

(dstest test-filter-fn
  (is (fn? (filter-fn 'continent 'name)))
  (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
    (is (= [continent] ((filter-fn 'continent 'name) "Europe")))
    (is (= [continent] ((filter-fn "continent" "name") "Europe")))))

(dstest test-find-all-continents-by-name
  (deffilter continent find-all-continents-by-name
    "Find all continents by name."
    (name))
  (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
    (is (= [continent] (find-all-continents-by-name (:name continent))))))

(dstest test-find-continent-by-name
  (deffilter continent find-continent-by-name
    "Find all countries by name." (name) first)
  (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
    (is (= continent (find-continent-by-name (:name continent))))))

(dstest test-def-find-all-by-property-fns
  (def-find-all-by-property-fns continent name iso-3166-alpha-2)
  (let [continent (ds/create-entity {:kind "continent" :name "Europe" :iso-3166-alpha-2 "eu"})]
    (is (= [continent] (find-all-continents-by-name (:name continent))))
    (is (= [continent] (find-all-continents-by-iso-3166-alpha-2 (:iso-3166-alpha-2 continent))))))

(dstest test-def-find-first-by-property-fns
  (def-find-first-by-property-fns continent name iso-3166-alpha-2)
  (let [continent (ds/create-entity {:kind "continent" :name "Europe" :iso-3166-alpha-2 "eu"})]
    (is (= continent (find-continent-by-name (:name continent))))
    (is (= continent (find-continent-by-iso-3166-alpha-2 (:iso-3166-alpha-2 continent))))))

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

(dstest test-make-region-key
  (let [continent {:key (make-continent-key {:iso-3166-alpha-2 "eu"})}
        country {:key (make-country-key continent {:iso-3166-alpha-2 "es"})}
        key (make-region-key country {:code "es.58"})]
    (is (= (class key) com.google.appengine.api.datastore.Key))
    (is (.isComplete key))
    (is (= (.getParent key) (:key country)))    
    (is (= (.getKind key) "region"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "es.58"))))

(dstest test-make-key-fn-without-keys
  (def-key-fn planet [])
  (= (nil? (make-planet-key "Earth"))))

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

(dstest test-def-create-fn-with-create-region
  (let [continent (create-continent {:iso-3166-alpha-2 "eu" :name "Europe"})
        country (create-country continent {:iso-3166-alpha-2 "es" :name "Spain"})
        region (create-region country {:code "es.58" :name "Galicia"})]
    (let [key (:key region)]
      (is (= (class key) com.google.appengine.api.datastore.Key))
      (is (.isComplete key))
      (is (= (.getParent key) (:key country)))    
      (is (= (.getKind key) "region"))
      (is (= (.getId key) 0))
      (is (= (.getName key) "es.58")))
    (is (= (:kind region) "region"))
    (is (= (:code region) "es.58"))
    (is (= (:name region) "Galicia"))))

(dstest test-def-make-fn-continent-with-continent
  (def-key-fn continent (:iso-3166-alpha-2))
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

(dstest test-def-delete-fn
  (def-delete-fn continent)
  (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
    (delete-continent continent)
    (is (nil? (find-continent-by-name "Europe")))))

(dstest test-def-find-all-fn
  (def-find-all-fn continent)
  (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
    (is (= (find-continents) [continent]))))

(dstest test-def-update-fn-with-continent
  (let [continent (ds/create-entity {:kind "continent" :name "unknown"})]
    (is (= (update-continent continent {:name "Europe"})
           (find-continent-by-name "Europe")))
    (update-continent continent {:name "Europe"})
    (is (= (count (find-continents)) 1))))

(dstest test-def-update-fn-with-country
  (let [continent (create-continent {:name "Europe" :iso-3166-alpha-2 "eu"})
        country (create-country continent {:name "unknown" :iso-3166-alpha-2 "es"})]
    (is (= (update-country country {:name "Spain"})
           (find-country-by-name "Spain")))
    (update-country country {:name "Spain"})
    (is (= (count (find-countries)) 1))))

(dstest test-property-finder
  (deffilter continent find-all-continents-by-name
    "Find all continents by name." (name))
  (is (fn? find-all-continents-by-name))
  (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
    (is (= [continent] (find-all-continents-by-name (:name continent))))))

;; note: if you don't set the :key, let the datastore set it for us
(defentity testuser ()
  (name)
  (job))

(dstest entities-without-key-param
  ;; create two of the same -- yet they are different because
  ;; the appengine will give each its own unique key
  (let [user (create-testuser {:name "liz" :job "entrepreneur"})
	user2 (create-testuser {:name "liz" :job "entrepreneur"})]
    (is (= (:name user) (:name user2)))
    (is (= (:job user) (:job user2)))
    (is (not= user user2))
    ;; can't make a key from an entity without a :key in its definition
    (is (nil? (make-testuser-key {:name "liz" :job "entrepreneur"})))
    (is (= (make-testuser {:name "liz" :job "entrepreneur"})
	   {:kind "testuser" :name "liz" :job "entrepreneur"}))
    (update-testuser user2 {:name "bob"})
    ;; can still do queries, make sure both users come
    ;; back as entrepreneurs
    (let [entrepreneurs (find-all-testusers-by-job "entrepreneur")]
      (is (= [true true] (reduce 
			  #(vector (or (= (:name %2) "liz") (first %1))
				   (or (= (:name %2) "bob") (second %1)))
			  [false false] entrepreneurs))))))
