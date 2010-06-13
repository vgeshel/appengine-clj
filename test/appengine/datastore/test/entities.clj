(ns appengine.datastore.test.entities
  (:import (com.google.appengine.api.datastore Entity Key GeoPt))
  (:use appengine.datastore.entities
        appengine.datastore.keys
        appengine.test
        clojure.test
        [clojure.contrib.string :only (join lower-case)]))

(refer-private 'appengine.datastore.entities)

(defentity Continent ()
  (iso-3166-alpha-2 :key lower-case)
  (location :type GeoPt)
  (name))

(defentity Country (Continent)
  (iso-3166-alpha-2 :key lower-case)
  (location :type GeoPt)
  (name))

(defentity Region (Country)
  (country-id :key lower-case)
  (location :type GeoPt)
  (name :key lower-case))

(defn make-europe []
  (make-continent :iso-3166-alpha-2 "eu" :name "Europe" :location {:latitude 54.52 :longitude 15.25}))

(defn make-germany []
  (make-country (make-europe) :iso-3166-alpha-2 "de" :name "Germany" :location {:latitude 51.16 :longitude 10.45}))

(defn make-berlin []
  (make-region (make-germany) :country-id "de" :name "Berlin" :location {:latitude 52.52 :longitude 13.41}))

(def *continent-specification*
     ['(iso-3166-alpha-2 :key #'lower-case)
      '(location :type GeoPt)
      '(name)])

(def *region-specification*
     ['(country-id :key #'lower-case)
      '(location :type GeoPt)
      '(name :key #'lower-case)])

(def *europe*
     {:iso-3166-alpha-2 "eu"
      :name "Europe"
      :location {:latitude 54.5260, :longitude 15.2551}})

(defn continent-meta-data []
  {:kind "continent"
   :properties
   {:name {:type String}
    :iso-3166-alpha-2 {:type String :key #'lower-case}
    :location {:type GeoPt}}})

(defn region-meta-data []
  {:kind "region"
   :properties
   {:country-id {:type String :key #'lower-case}
    :name {:type String :key #'lower-case}
    :location {:type GeoPt}}})

(defn make-example-continent []
  (with-meta
    {:key (create-key "continent" "eu")
     :iso-3166-alpha-2 "eu"
     :name "Europe"
     :location {:latitude 54.5260, :longitude 15.2551}}
    (continent-meta-data)))

(datastore-test test-blank-entity-with-key  
  (let [entity (blank-entity (create-key "continent" 1))]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) "continent"))
    (is (nil? (.getParent entity)))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (isa? (class key) Key))
      (is (= (.getKind key) "continent"))
      (is (nil? (.getParent key)))
      (is (= (.getId key) 1))
      (is (nil? (.getName key)))
      (is (.isComplete key)))))

(datastore-test test-blank-entity-with-kind
  (let [entity (blank-entity "continent")]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) "continent"))
    (is (nil? (.getParent entity)))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (isa? (class key) Key))
      (is (= (.getKind key) "continent"))
      (is (nil? (.getParent key)))
      (is (= (.getId key) 0))
      (is (nil? (.getName key)))
      (is (not (.isComplete key))))))

(datastore-test test-blank-entity-with-key
  (let [key (create-key "continent" "eu") entity (blank-entity key)]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) "continent"))
    (is (nil? (.getParent entity)))
    (is (empty? (.getProperties entity)))
    (is (= (.getKey entity) key))))

(deftest test-entity?-fn
  (are [record name]
    (is (= (entity?-fn record) name))
    'Continent 'continent?
    'CountryFlag 'country-flag?))

(deftest test-make-entity-key-fn
  (are [record name]
    (is (= (make-entity-key-fn record) name))
    'Continent 'make-continent-key
    'CountryFlag 'make-country-flag-key))

(deftest test-make-entity-fn
  (are [record name]
    (is (= (make-entity-fn record) name))
    'Continent 'make-continent
    'CountryFlag 'make-country-flag))

(datastore-test test-blank-entity-with-parent-kind-and-named-key
  (let [parent-key (create-key "continent" "eu")
        entity (blank-entity parent-key "country" "de")]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) "country"))
    (is (= (.getParent entity) parent-key))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (isa? (class key) Key))
      (is (= (.getKind key) "country"))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de"))
      (is (.isComplete key))
      (is (= (.getParent key) parent-key)))))

(datastore-test test-property-class-with-meta  
  (let [continent (make-example-continent)]
    (are [property class]
      (is (= (property-class continent property) class))
      :iso-3166-alpha-2 String
      :key Key
      :location GeoPt
      :name String)))

(datastore-test test-property-class-without-meta  
  (let [continent (with-meta (make-example-continent) {})]
    (are [property class]
      (is (= (property-class continent property) class))
      :iso-3166-alpha-2 String
      :key Key
      :location clojure.lang.PersistentArrayMap
      :name String)))

(datastore-test test-continent?
  (is (not (continent? nil)))
  (is (not (continent? "")))
  (let [continent (make-europe)]
    (is (continent? continent))
    (is (not (continent? (make-germany))))))

(deftest test-extract-properties
  (let [properties (extract-properties *continent-specification*)]      
    (is (= (:iso-3166-alpha-2 properties) {:key '#'lower-case}))
    (is (= (:location properties) {:type 'GeoPt}))
    (is (= (:name properties) {}))))

(deftest test-extract-key-fns
  (is (= (extract-key-fns *continent-specification*)
         [[:iso-3166-alpha-2 '#'lower-case]]))
  (is (= (extract-key-fns *region-specification*)
         [[:country-id '#'lower-case] [:name '#'lower-case]])))

(deftest test-extract-meta-data
  (let [meta (extract-meta-data 'Continent *continent-specification*)]
    (is (= (:key-fns meta) [[:iso-3166-alpha-2 '#'lower-case]]))
    (is (= (:kind meta) "continent"))
    (let [properties (:properties meta)]      
      (is (= (:iso-3166-alpha-2 properties) {:key '#'lower-case}))
      (is (= (:location properties) {:type 'GeoPt}))
      (is (= (:name properties) {}))))
  (let [meta (extract-meta-data 'Region *region-specification*)]
    (is (= (:key-fns meta) [[:country-id '#'lower-case] [:name '#'lower-case]]))
    (is (= (:kind meta) "region"))
    (let [properties (:properties meta)]      
      (is (= (:country-id properties) {:key '#'lower-case}))
      (is (= (:location properties) {:type 'GeoPt}))
      (is (= (:name properties) {:key '#'lower-case})))))

(datastore-test test-make-continent-key
  (let [key (make-continent-key :iso-3166-alpha-2 "eu")]
    (is (key? key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))    
    (is (= (.getKind key) "continent"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "eu"))))

(datastore-test test-make-country-key
  (let [continent-key (create-key "continent" "eu")
        key (make-country-key continent-key :iso-3166-alpha-2 "de")]
    (is (key? key))
    (is (.isComplete key))    
    (is (= (.getParent key) continent-key))    
    (is (= (.getKind key) "country"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de"))))

(datastore-test test-make-region-key
  (let [continent-key (create-key "continent" "eu")
        country-key (make-country-key continent-key :iso-3166-alpha-2 "de")
        key (make-region-key country-key :country-id "de" :name "Berlin")]
    (is (key? key))
    (is (.isComplete key))    
    (is (= (.getParent key) country-key))    
    (is (= (.getParent (.getParent key)) continent-key))    
    (is (= (.getKind key) "region"))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de-berlin"))))

(datastore-test test-make-continent
  (let [location {:latitude 54.52 :longitude 15.25}
        continent (make-continent :iso-3166-alpha-2 "eu" :name "Europe" :location location)]
    (let [key (:key continent)]
      (is (key? key))
      (is (.isComplete key))    
      (is (nil? (.getParent key)))    
      (is (= (.getKind key) "continent"))
      (is (= (.getId key) 0))
      (is (= (.getName key) "eu")))
    (is (= (:iso-3166-alpha-2 continent) "eu"))
    (is (= (:kind continent) (.getKind (:key continent))))
    (is (= (:location continent) location))
    (is (= (:name continent) "Europe"))))

(datastore-test test-make-country
  (let [location {:latitude 51.16 :longitude 10.45}
        country (make-country (make-europe) :iso-3166-alpha-2 "de" :name "Germany" :location location)]
    (let [key (:key country)]
      (is (key? key))
      (is (.isComplete key))    
      (is (= (.getParent key) (:key (make-europe))))    
      (is (= (.getKind key) "country"))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de")))
    (is (= (:iso-3166-alpha-2 country) "de"))
    (is (= (:kind country) (.getKind (:key country))))
    (is (= (:location country) location))
    (is (= (:name country) "Germany"))))

(datastore-test test-make-region
  (let [location {:latitude 52.52 :longitude 13.41}
        region (make-region (make-germany) :country-id "de" :name "Berlin" :location location)]
    (let [key (:key region)]
      (is (key? key))
      (is (.isComplete key))    
      (is (= (.getParent key) (:key (make-germany))))    
      (is (= (.getKind key) "region"))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de-berlin")))
    (is (= (:country-id region) "de"))
    (is (= (:kind region) (.getKind (:key region))))
    (is (= (:location region) location))
    (is (= (:name region) "Berlin"))))

;; (let [continent {:iso-3166-alpha-2 "eu" :name "Europe"}]
;;   (join
;;    "-"
;;    (map
;;     (fn [[key transform]]
;;       (if-let [value (key continent)]
;;         (transform value)))
;;     (property-key-fns (continent-meta-data)))))

;; (let [continent {:iso-3166-alpha-2 "eu" :name "Europe"}]
;;   (join "-" (map #(if-let [value ((first %) continent)] ((last %) value))
;;                  (property-key-fns (continent-meta-data)))))

;; (let [continent {:iso-3166-alpha-2 "eu" :name "Europe"}]
;;   (join "-" (map #(if-let [value ((first %) continent)]
;;                     ((last %) value)
;;                     (throw (IllegalArgumentException. (str "Missing key: " (first %)))))
;;                  (property-key-fns (continent-meta-data)))))

;; (apply named-key
;;  {:iso-3166-alpha-2 "eu" :name "Europe"}
;;  (property-key-fns (continent-meta-data)))

;; (apply named-key
;;  {:iso-3166-alpha-2 "eu" :name "Europe"}
;;  [])

;;  (property-key-fns (continent-meta-data))

;;  (property-key-fns (continent-meta-data))

;; (let [continent {:iso-3166-alpha-2 "eu" :name "Europe"}]
;;   (join "-" (map
;;              (fn [[key transform-key-fn]]
;;                (if-let [value (key continent)]
;;                  (transform-key-fn value)
;;                  (throw (IllegalArgumentException. (str "Missing key: " key)))))
;;              (property-key-fns (continent-meta-data)))))

;; (let [properties {:iso-3166-alpha-2 "eu" :name "Europe"}]
;;   (join "-" (map #(if-let [value ((last %) ((first %) properties))]
;;                     value
;;                     (throw (IllegalArgumentException. "Key is missing:" (str (first )))))
;;                  (property-key-fns (continent-meta-data)))))

;; (property-key-fns (continent-meta-data))



;; (println (property-meta-data ['(iso-3166-alpha-2 :key true) '(location :type GeoPt) '(name)]))

;; (make-record-fn-sym 'Contient)

;; (datastore-test test-serialize-property-fn-with-meta
;;   (let [continent (make-example-continent)]
;;     (println ((serialze-property-fn continent :name) (:name continent)))
;;     (println ((serialze-property-fn continent :location) (:location continent)))
;;     (are [property class]
;;       (is (= (property-type continent property) class))
;;       :iso-3166-alpha-2 String
;;       :key Key
;;       :location clojure.lang.PersistentArrayMap
;;       :name String))
;;   )

;; (defentity continent ()
;;   (iso-3166-alpha-2 :key true)
;;   (location :type GeoPt)
;;   (name))

;; (defentity country (continent)
;;   (iso-3166-alpha-2 :key true)
;;   (location :type GeoPt)
;;   (name))

;; (defentity region (country)
;;   (code :key true)
;;   (location :type GeoPt)
;;   (name))

;; (deftest test-entity-key?
;;   (is (entity-key? '(iso-3166-alpha-2 :key true)))
;;   (is (not (entity-key? '(iso-3166-alpha-2))))
;;   (is (not (entity-key? '(iso-3166-alpha-2 :key false)))))

;; (deftest test-entity-keys
;;   (is (= (entity-keys '((iso-3166-alpha-2 :key true) (name :key false)))
;;          ['iso-3166-alpha-2])))

;; (deftest test-find-entities-fn-doc
;;   (is (= (find-entities-fn-doc 'country 'iso-3166-alpha-2)
;;          "Find all countries by iso-3166-alpha-2."))
;;   (is (= (find-entities-fn-doc 'sheep 'color)
;;          "Find all sheep by color.")))

;; (deftest test-find-entities-fn-name
;;   (is (= (find-entities-fn-name 'country 'iso-3166-alpha-2)
;;          "find-all-countries-by-iso-3166-alpha-2"))
;;   (is (= (find-entities-fn-name 'sheep 'color)
;;          "find-all-sheep-by-color")))

;; (deftest test-find-entity-fn-doc
;;   (is (= (find-entity-fn-doc 'country 'iso-3166-alpha-2)
;;          "Find the first country by iso-3166-alpha-2."))
;;   (is (= (find-entity-fn-doc 'sheep 'color)
;;          "Find the first sheep by color.")))

;; (deftest test-find-entity-fn-name
;;   (is (= (find-entity-fn-name 'country 'iso-3166-alpha-2)
;;          "find-country-by-iso-3166-alpha-2"))
;;   (is (= (find-entity-fn-name 'sheep 'color)
;;          "find-sheep-by-color")))

;; (datastore-test test-filter-query
;;   (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
;;     (is (= [continent] (ds/find-all (filter-query 'continent 'name "Europe"))))
;;     (is (= [continent] (ds/find-all (filter-query "continent" "name" "Europe"))))))

;; (datastore-test test-filter-fn
;;   (is (fn? (filter-fn 'continent 'name)))
;;   (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
;;     (is (= [continent] ((filter-fn 'continent 'name) "Europe")))
;;     (is (= [continent] ((filter-fn "continent" "name") "Europe")))))

;; (datastore-test test-find-all-continents-by-name
;;   (deffilter continent find-all-continents-by-name
;;     "Find all continents by name."
;;     (name))
;;   (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
;;     (is (= [continent] (find-all-continents-by-name (:name continent))))))

;; (datastore-test test-find-continent-by-name
;;   (deffilter continent find-continent-by-name
;;     "Find all countries by name." (name) first)
;;   (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
;;     (is (= continent (find-continent-by-name (:name continent))))))

;; (datastore-test test-def-find-all-by-property-fns
;;   (def-find-all-by-property-fns continent name iso-3166-alpha-2)
;;   (let [continent (ds/create-entity {:kind "continent" :name "Europe" :iso-3166-alpha-2 "eu"})]
;;     (is (= [continent] (find-all-continents-by-name (:name continent))))
;;     (is (= [continent] (find-all-continents-by-iso-3166-alpha-2 (:iso-3166-alpha-2 continent))))))

;; (datastore-test test-def-find-first-by-property-fns
;;   (def-find-first-by-property-fns continent name iso-3166-alpha-2)
;;   (let [continent (ds/create-entity {:kind "continent" :name "Europe" :iso-3166-alpha-2 "eu"})]
;;     (is (= continent (find-continent-by-name (:name continent))))
;;     (is (= continent (find-continent-by-iso-3166-alpha-2 (:iso-3166-alpha-2 continent))))))

;; (deftest test-key-fn-name
;;   (is (= (key-fn-name 'continent) "make-continent-key")))

;; (datastore-test test-make-continent-key
;;   (def-key-fn continent (:iso-3166-alpha-2))
;;   (let [key (make-continent-key {:iso-3166-alpha-2 "eu"})]
;;     (is (= (class key) com.google.appengine.api.datastore.Key))
;;     (is (.isComplete key))
;;     (is (nil? (.getParent key)))    
;;     (is (= (.getKind key) "continent"))
;;     (is (= (.getId key) 0))
;;     (is (= (.getName key) "eu"))))

;; (datastore-test test-make-country-key
;;   (def-key-fn country (:iso-3166-alpha-2) continent)
;;   (let [continent {:key (ds/create-key "continent" "eu")}
;;         key (make-country-key continent {:iso-3166-alpha-2 "es"})]
;;     (is (= (class key) com.google.appengine.api.datastore.Key))
;;     (is (.isComplete key))
;;     (is (= (.getParent key) (:key continent)))    
;;     (is (= (.getKind key) "country"))
;;     (is (= (.getId key) 0))
;;     (is (= (.getName key) "es"))))

;; (datastore-test test-make-region-key
;;   (let [continent {:key (make-continent-key {:iso-3166-alpha-2 "eu"})}
;;         country {:key (make-country-key continent {:iso-3166-alpha-2 "es"})}
;;         key (make-region-key country {:code "es.58"})]
;;     (is (= (class key) com.google.appengine.api.datastore.Key))
;;     (is (.isComplete key))
;;     (is (= (.getParent key) (:key country)))    
;;     (is (= (.getKind key) "region"))
;;     (is (= (.getId key) 0))
;;     (is (= (.getName key) "es.58"))))

;; (datastore-test test-make-key-fn-without-keys
;;   (def-key-fn planet [])
;;   (= (nil? (make-planet-key "Earth"))))

;; (datastore-test test-def-create-fn-with-continent
;;   (def-key-fn continent (:iso-3166-alpha-2))
;;   (def-make-fn continent () (iso-3166-alpha-2 :key true) (name))
;;   (def-create-fn continent)
;;   (let [continent (create-continent {:iso-3166-alpha-2 "eu" :name "Europe"})]
;;     (let [key (:key continent)]
;;       (is (= (class key) com.google.appengine.api.datastore.Key))
;;       (is (.isComplete key))
;;       (is (nil? (.getParent key)))    
;;       (is (= (.getKind key) "continent"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "eu")))
;;     (is (= (:kind continent) "continent"))
;;     (is (= (:iso-3166-alpha-2 continent) "eu"))
;;     (is (= (:name continent) "Europe"))))

;; (datastore-test test-def-create-fn-with-create-country
;;   (def-key-fn continent (:iso-3166-alpha-2))
;;   (def-make-fn continent () (iso-3166-alpha-2 :key true) (name))
;;   (def-create-fn continent)
;;   (def-key-fn country (:iso-3166-alpha-2) continent)
;;   (def-make-fn country (continent) (iso-3166-alpha-2 :key true) (iso-3166-alpha-3) (name))
;;   (def-create-fn country continent)
;;     (let [continent (create-continent {:iso-3166-alpha-2 "eu" :name "Europe"})
;;           country (create-country continent {:iso-3166-alpha-2 "es" :name "Spain"})]
;;     (let [key (:key country)]
;;       (is (= (class key) com.google.appengine.api.datastore.Key))
;;       (is (.isComplete key))
;;       (is (= (.getParent key) (:key continent)))    
;;       (is (= (.getKind key) "country"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "es")))
;;     (is (= (:kind country) "country"))
;;     (is (= (:iso-3166-alpha-2 country) "es"))
;;     (is (= (:name country) "Spain"))))

;; (datastore-test test-def-create-fn-with-create-region
;;   (let [continent (create-continent {:iso-3166-alpha-2 "eu" :name "Europe"})
;;         country (create-country continent {:iso-3166-alpha-2 "es" :name "Spain"})
;;         region (create-region country {:code "es.58" :name "Galicia"})]
;;     (let [key (:key region)]
;;       (is (= (class key) com.google.appengine.api.datastore.Key))
;;       (is (.isComplete key))
;;       (is (= (.getParent key) (:key country)))    
;;       (is (= (.getKind key) "region"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "es.58")))
;;     (is (= (:kind region) "region"))
;;     (is (= (:code region) "es.58"))
;;     (is (= (:name region) "Galicia"))))

;; (datastore-test test-def-make-fn-continent-with-continent
;;   (def-key-fn continent (:iso-3166-alpha-2))
;;   (def-make-fn continent () (iso-3166-alpha-2 :key true) (name))
;;   (let [continent (make-continent {:iso-3166-alpha-2 "eu" :name "Europe"})]
;;     (let [key (:key continent)]
;;       (is (= (class key) com.google.appengine.api.datastore.Key))
;;       (is (.isComplete key))
;;       (is (nil? (.getParent key)))    
;;       (is (= (.getKind key) "continent"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "eu")))
;;     (is (= (:kind continent) "continent"))
;;     (is (= (:iso-3166-alpha-2 continent) "eu"))
;;     (is (= (:name continent) "Europe"))))

;; (datastore-test test-def-make-fn-with-undefined-attribute
;;   (def-key-fn continent (:iso-3166-alpha-2))
;;   (def-make-fn continent () (iso-3166-alpha-2 :key true) (name))
;;   (let [continent (make-continent {:iso-3166-alpha-2 "eu" :name "Europe" :undefined "UNDEFINED"})]
;;     (is (not (contains? continent :undefined )))))

;; (datastore-test test-def-make-fn-with-country
;;   (def-make-fn continent () (iso-3166-alpha-2 :key true) (name))
;;   (def-make-fn country (continent) (iso-3166-alpha-2 :key true) (iso-3166-alpha-3) (name))
;;   (let [continent (make-continent {:iso-3166-alpha-2 "eu" :name "Europe"})
;;         country (make-country continent {:iso-3166-alpha-2 "es" :name "Spain"})]
;;     (let [key (:key country)]
;;       (is (= (class key) com.google.appengine.api.datastore.Key))
;;       (is (.isComplete key))
;;       (is (= (.getParent key) (:key continent)))    
;;       (is (= (.getKind key) "country"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "es")))
;;     (is (= (:kind country) "country"))
;;     (is (= (:iso-3166-alpha-2 country) "es"))
;;     (is (= (:name country) "Spain"))))

;; (datastore-test test-def-delete-fn
;;   (def-delete-fn continent)
;;   (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
;;     (delete-continent continent)
;;     (is (nil? (find-continent-by-name "Europe")))))

;; (datastore-test test-def-find-all-fn
;;   (def-find-all-fn continent)
;;   (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
;;     (is (= (find-continents) [continent]))))

;; (datastore-test test-def-update-fn-with-continent
;;   (let [continent (ds/create-entity {:kind "continent" :name "unknown"})]
;;     (is (= (update-continent continent {:name "Europe"})
;;            (find-continent-by-name "Europe")))
;;     (update-continent continent {:name "Europe"})
;;     (is (= (count (find-continents)) 1))))

;; (datastore-test test-def-update-fn-with-country
;;   (let [continent (create-continent {:name "Europe" :iso-3166-alpha-2 "eu"})
;;         country (create-country continent {:name "unknown" :iso-3166-alpha-2 "es"})]
;;     (is (= (update-country country {:name "Spain"})
;;            (find-country-by-name "Spain")))
;;     (update-country country {:name "Spain"})
;;     (is (= (count (find-countries)) 1))))

;; (datastore-test test-property-finder
;;   (deffilter continent find-all-continents-by-name
;;     "Find all continents by name." (name))
;;   (is (fn? find-all-continents-by-name))
;;   (let [continent (ds/create-entity {:kind "continent" :name "Europe"})]
;;     (is (= [continent] (find-all-continents-by-name (:name continent))))))

;; ;; note: if you don't set the :key, let the datastore set it for us
;; (defentity testuser ()
;;   (name)
;;   (job))

;; (datastore-test entities-without-key-param
;;   ;; create two of the same -- yet they are different because
;;   ;; the appengine will give each its own unique key
;;   (let [user (create-testuser {:name "liz" :job "entrepreneur"})
;; 	user2 (create-testuser {:name "liz" :job "entrepreneur"})]
;;     (is (= (:name user) (:name user2)))
;;     (is (= (:job user) (:job user2)))
;;     (is (not= user user2))
;;     ;; can't make a key from an entity without a :key in its definition
;;     (is (nil? (make-testuser-key {:name "liz" :job "entrepreneur"})))
;;     (is (= (make-testuser {:name "liz" :job "entrepreneur"})
;; 	   {:kind "testuser" :name "liz" :job "entrepreneur"}))
;;     (update-testuser user2 {:name "bob"})
;;     ;; can still do queries, make sure both users come
;;     ;; back as entrepreneurs
;;     (let [entrepreneurs (find-all-testusers-by-job "entrepreneur")]
;;       (is (= [true true] (reduce 
;; 			  #(vector (or (= (:name %2) "liz") (first %1))
;; 				   (or (= (:name %2) "bob") (second %1)))
;; 			  [false false] entrepreneurs))))))


;; (with-local-datastore
;;   (let [location {:latitude 54.5260, :longitude 15.2551}
;;         continent (make-continent :name "Europe" :iso-3166-alpha-2 "eu" :location location)
;;         country (make-country continent :name "Germany" :iso-3166-alpha-2 "de" :location location)]
;;     (println continent)
;;     (println country)
;;     ))
