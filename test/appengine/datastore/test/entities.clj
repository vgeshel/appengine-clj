(ns appengine.datastore.test.entities
  (:import (com.google.appengine.api.datastore Entity Key GeoPt))
  (:use appengine.datastore.entities
        appengine.datastore.keys
        appengine.datastore.protocols
        appengine.test
        appengine.utils
        clojure.test
        [clojure.contrib.string :only (join lower-case)]
        [clojure.contrib.seq :only (includes?)]))

(refer-private 'appengine.datastore.entities)

(defentity Person ()
  ((name)))

(defentity Continent ()
  ((iso-3166-alpha-2 :key lower-case)
   (location :serialize GeoPt)
   (name)))

;; (with-local-datastore
;;   (create (continent :iso-3166-alpha-2 "eu" :name "Europe")))

;; (with-local-datastore
;;   (create (europe-entity)))

;; (with-local-datastore
;;   (continent-key { :iso-3166-alpha-2 "eu" :name "Europe"}))

;; (with-local-datastore
;;   (country-key
;;    (continent-key :iso-3166-alpha-2 "eu" :name "Europe")
;;    { :iso-3166-alpha-2 "de" :name "Germany"}
;;    ))

(defentity Country (Continent)
  ((iso-3166-alpha-2 :key lower-case)
   (location :serialize GeoPt)
   (name)))

(defentity Region (Country)
  ((country-id :key lower-case)
   (location :serialize GeoPt)
   (name :key lower-case)))

(defn europe-seq []
  [:iso-3166-alpha-2 "eu"
   :key (make-key (entity-kind Continent) "eu")
   :kind (entity-kind Continent)
   :location {:latitude (float 54.52) :longitude (float 15.25)}
   :name "Europe"])

(defn europe-array-map []
  (apply array-map (europe-seq)) )

(defn europe-hash-map []
  (apply hash-map (europe-seq)) )

(defn europe-entity []
  (let [{:keys [key name iso-3166-alpha-2 location]} (europe-hash-map)]
    (doto (Entity. key)
      (.setProperty "name" name)
      (.setProperty "iso-3166-alpha-2" iso-3166-alpha-2)
      (.setProperty "location" (GeoPt. (:latitude location) (:longitude location))))))

(defn europe-record []
  (continent
   :iso-3166-alpha-2 "eu"
   :key (make-key (entity-kind Continent) "eu")
   :kind (entity-kind Continent)
   :location {:latitude (float 54.52) :longitude (float 15.25)}
   :name "Europe"))

(defn make-germany []
  (country
   (europe-record)
   :iso-3166-alpha-2 "de"
   :location {:latitude (float 51.16) :longitude (float 10.45)}
   :name "Germany"))

(defn make-berlin []
  (region
   (make-germany)
   :country-id "de"
   :location {:latitude (float 52.52) :longitude (float 13.41)}
   :name "Berlin"))

(def continent-specification
     ['(iso-3166-alpha-2 :key #'lower-case)
      '(location :serialize GeoPt)
      '(name)])

(def region-specification
     ['(country-id :key #'lower-case)
      '(location :serialize GeoPt)
      '(name :key #'lower-case)])

(datastore-test test-continent?
  (is (not (continent? nil)))
  (is (not (continent? "")))
  (is (not (continent? (make-germany))))
  (is (continent? (europe-array-map)))
  (is (continent? (europe-entity)))
  (is (continent? (europe-hash-map)))
  (is (continent? (europe-record))))

(datastore-test test-make-blank-entity-with-named-key
  (let [key (make-key "continent" "eu")
        entity (make-blank-entity key)]
    (is (= (.getKey entity) key))
    (is (= (.getKind entity) "continent"))
    (is (empty? (.getProperties entity)))
    (is (isa? (class entity) Entity))
    (is (nil? (.getParent entity)))))

(datastore-test test-make-blank-entity-with-numbered-key 
  (let [key (make-key "continent" 1)
        entity (make-blank-entity key)]
    (is (= (.getKey entity) key))
    (is (= (.getKind entity) "continent"))
    (is (empty? (.getProperties entity)))
    (is (isa? (class entity) Entity))
    (is (nil? (.getParent entity)))))

(datastore-test test-make-blank-entity-with-kind
  (let [entity (make-blank-entity "continent")]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) "continent"))
    (is (nil? (.getParent entity)))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (= (.getId key) 0))
      (is (= (.getKind key) "continent"))
      (is (isa? (class key) Key))
      (is (nil? (.getName key)))
      (is (nil? (.getParent key)))
      (is (not (.isComplete key))))))

(datastore-test test-make-blank-entity-with-parent-kind-and-named-key
  (let [parent-key (make-key "continent" "eu")
        entity (make-blank-entity parent-key "country" "de")]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) "country"))
    (is (= (.getParent entity) parent-key))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (.isComplete key))
      (is (= (.getId key) 0))
      (is (= (.getKind key) "country"))
      (is (= (.getName key) "de"))
      (is (= (.getParent key) parent-key))
      (is (isa? (class key) Key)))))

(datastore-test test-make-blank-entity-with-parent-kind-and-numbered-key
  (let [parent-key (make-key "continent" "eu")
        entity (make-blank-entity parent-key "country" 1)]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) "country"))
    (is (= (.getParent entity) parent-key))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (.isComplete key))
      (is (= (.getId key) 1))
      (is (= (.getKind key) "country"))
      (is (= (.getParent key) parent-key))
      (is (isa? (class key) Key))
      (is (nil? (.getName key))))))

(datastore-test test-entity->map
  (let [entity (Entity. (make-key "color" "red")) map (entity->map entity)]
    (is (map? map))
    (is (= (count (keys map)) (+ 2 (count (.getProperties entity)))))
    (is (= (:key map) (.getKey entity)))
    (is (= (:kind map) (.getKind entity)))))

(datastore-test test-entity->map-with-continent
  (let [entity (europe-entity) map (entity->map entity)]
    (is (map? map))
    (is (= (count (keys map)) (+ 2 (count (.getProperties entity)))))
    (is (= (:key map) (.getKey entity)))
    (is (= (:kind map) (.getKind entity)))
    (is (= (:iso-3166-alpha-2 map) (.getProperty entity "iso-3166-alpha-2")))
    (is (= (:name map) (.getProperty entity "name"))) 
    (is (= (:location map) (deserialize (.getProperty entity "location"))))))

(datastore-test test-map->entity-with-person
  (let [entity (map->entity {:kind "person" :name "Roman"})]
    (is (entity? entity))
    (is (= (.getKind entity) "person"))
    (is (= (.getProperty entity "name") "Roman"))
    (let [key (.getKey entity)]
      (is (= (.getId key) 0))
      (is (= (.getKind key) "person"))
      (is (isa? (class key) Key))
      (is (nil? (.getName key)))
      (is (nil? (.getParent key)))
      (is (not (.isComplete key))))))

(datastore-test test-map->entity-with-color
  (let [entity (map->entity {:kind "color" :key (make-key "color" "red")})]
    (is (entity? entity))
    (is (= (.getKind entity) "color"))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (.isComplete key))
      (is (= (.getId key) 0))
      (is (= (.getKind key) "color"))
      (is (= (.getName key) "red"))
      (is (isa? (class key) Key))
      (is (nil? (.getParent key))))))

(datastore-test test-map->entity-with-continent
  (is (= (map->entity (europe-array-map)) (europe-entity)))
  (is (= (map->entity (europe-hash-map)) (europe-entity)))
  (is (= (map->entity (europe-record)) (europe-entity))))

(datastore-test test-entity?
  (is (not (entity? nil)))
  (is (not (entity? "")))
  (is (entity? (Entity. "person"))))

(deftest test-entity-p-fn-doc
  (are [record name]
    (is (= (entity-p-fn-doc record) name))
    Continent "Returns true if arg is a continent, false otherwise."
    'CountryFlag "Returns true if arg is a country flag, false otherwise."))

(deftest test-entity-p-fn-sym
  (are [record name]
    (is (= (entity-p-fn-sym record) name))
    Continent 'continent?
    'CountryFlag 'country-flag?))

(deftest test-entity-kind
  (are [entity kind]
    (is (= (entity-kind entity) kind))
    nil nil
    Continent "continent"
    "Continent" "continent"))

(deftest test-find-entities-fn-sym
  (are [record name]
    (is (= (find-entities-fn-sym record) name))
    Continent 'find-continents
    'CountryFlag 'find-country-flags))

(deftest test-find-entities-property-fn-sym
  (are [record property name]
    (is (= (find-entities-by-property-fn-sym record property) name))
    Continent 'iso-3166-alpha-2 'find-continents-by-iso-3166-alpha-2
    'CountryFlag 'iso-3166-alpha-2 'find-country-flags-by-iso-3166-alpha-2))

(deftest test-key-fn-doc
  (are [record name]
    (is (= (key-fn-doc record) name))
    Continent "Make a continent key."
    'CountryFlag "Make a country flag key."))

(deftest test-key-fn-sym
  (are [record name]
    (is (= (key-fn-sym record) name))
    Continent 'continent-key
    'CountryFlag 'country-flag-key))

(deftest test-key-name-fn-doc
  (are [record name]
    (is (= (key-name-fn-doc record) name))
    'Continent "Extract the continent key name."
    'CountryFlag "Extract the country flag key name."))

(deftest test-key-name-fn-sym
  (are [record name]
    (is (= (key-name-fn-sym record) name))
    Continent 'continent-key-name
    'CountryFlag 'country-flag-key-name))

(deftest test-entity-fn-doc
  (are [record name]
    (is (= (entity-fn-doc record) name))
    Continent "Make a continent."
    'CountryFlag "Make a country flag."))

(deftest test-entity-fn-sym
  (are [record name]
    (is (= (entity-fn-sym record) name))
    Continent 'continent
    'CountryFlag 'country-flag))

(deftest test-extract-deserializer
  (is (= (extract-deserializer continent-specification)
         {:location 'GeoPt})))

(deftest test-extract-key-fns
  (is (= (extract-key-fns continent-specification)
         [:iso-3166-alpha-2 '#'lower-case]))
  (is (= (extract-key-fns region-specification)
         [:country-id '#'lower-case :name '#'lower-case])))

(deftest test-extract-option
  (is (= (extract-option continent-specification :key)
         {:iso-3166-alpha-2 '(var lower-case)}))
  (is (= (extract-option continent-specification :serialize)
         {:location 'GeoPt})))

(deftest test-extract-properties
  (let [properties (extract-properties continent-specification)]      
    (is (= (:iso-3166-alpha-2 properties) {:key '#'lower-case}))
    (is (= (:location properties) {:serialize 'GeoPt}))
    (is (= (:name properties) {}))))

(deftest test-extract-serializer
  (is (= (extract-serializer continent-specification)
         {:location 'GeoPt})))

(datastore-test test-find-continents
  (let [europe (save (europe-record))
        continents (find-continents)]
    (is (seq? continents))
    (is (not (empty? continents)))
    (is (= (first continents) europe))))

(datastore-test test-find-continents-by-iso-3166-alpha-2
  (let [europe (save (europe-record))
        continents (find-continents-by-iso-3166-alpha-2 (:iso-3166-alpha-2 europe))]
    (is (seq? continents))
    (is (not (empty? continents)))
    (is (includes? continents europe))))

(datastore-test test-find-continents-by-name
  (let [europe (save (europe-record))
        continents (find-continents-by-name (:name europe))]
    (is (seq? continents))
    (is (not (empty? continents)))
    (is (includes? continents europe))))

(datastore-test test-find-continents-by-location
  (let [europe (save (europe-record))
        continents (find-continents-by-location (:location europe))]
    (is (seq? continents))
    (is (not (empty? continents)))
    (is (includes? continents europe))))

(deftest test-xxx-key-name
  (is (nil? (person-key-name :name "Roman")))
  (is (= (person-key-name :name "Roman")
         (person-key-name {:name "Roman"})))
  (is (= (continent-key-name :iso-3166-alpha-2 "eu") "eu"))
  (is (= (continent-key-name :iso-3166-alpha-2 "eu") "eu"
         (continent-key-name {:iso-3166-alpha-2 "eu"})))
  (is (= (country-key-name :iso-3166-alpha-2 "de") "de"))
  (is (= (country-key-name :iso-3166-alpha-2 "de")
         (country-key-name {:iso-3166-alpha-2 "de"})))
  (is (= (region-key-name :country-id "de" :name "Berlin") "de-berlin"))
  (is (= (region-key-name :country-id "de" :name "Berlin")
         (region-key-name {:country-id "de" :name "Berlin"}))))

(datastore-test test-person-key
  (is (nil? (person-key :name "Roman"))))

(datastore-test test-continent-key
  (let [key (continent-key :iso-3166-alpha-2 "eu")]
    (is (key? key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))    
    (is (= (.getKind key) (entity-kind Continent)))
    (is (= (.getId key) 0))
    (is (= (.getName key) "eu")))
  (is (= (continent-key :iso-3166-alpha-2 "eu")
         (continent-key {:iso-3166-alpha-2 "eu"}))))

(datastore-test test-country-key
  (let [continent-key (make-key "continent" "eu")]
    (let [country-key (country-key continent-key :iso-3166-alpha-2 "de")]
      (is (key? country-key))
      (is (.isComplete country-key))    
      (is (= (.getParent country-key) continent-key))    
      (is (= (.getKind country-key) "country"))
      (is (= (.getId country-key) 0))
      (is (= (.getName country-key) "de")))
    (is (= (country-key continent-key :iso-3166-alpha-2 "de")
           (country-key continent-key {:iso-3166-alpha-2 "de"})))))

(datastore-test test-region-key
  (let [continent-key (make-key "continent" "eu")
        country-key (country-key continent-key :iso-3166-alpha-2 "de")]
    (let [region-key (region-key country-key :country-id "de" :name "Berlin")]
      (is (key? region-key))
      (is (.isComplete region-key))    
      (is (= (.getParent region-key) country-key))    
      (is (= (.getParent (.getParent region-key)) continent-key))    
      (is (= (.getKind region-key) "region"))
      (is (= (.getId region-key) 0))
      (is (= (.getName region-key) "de-berlin")))
    (is (= (region-key country-key :country-id "de" :name "Berlin")
           (region-key country-key {:country-id "de" :name "Berlin"})))))

(datastore-test test-continent
  (let [location {:latitude 54.52 :longitude 15.25}
        continent (continent :iso-3166-alpha-2 "eu" :name "Europe" :location location)]
    (let [key (:key continent)]
      (is (key? key))
      (is (.isComplete key))    
      (is (nil? (.getParent key)))    
      (is (= (.getKind key) (entity-kind Continent)))
      (is (= (.getId key) 0))
      (is (= (.getName key) "eu")))
    (is (= (:iso-3166-alpha-2 continent) "eu"))
    (is (= (:kind continent) (.getKind (:key continent))))
    (is (= (:location continent) location))
    (is (= (:name continent) "Europe"))))

(datastore-test test-country
  (let [location {:latitude 51.16 :longitude 10.45}
        country (country (europe-record) :iso-3166-alpha-2 "de" :name "Germany" :location location)]
    (let [key (:key country)]
      (is (key? key))
      (is (.isComplete key))    
      (is (= (.getParent key) (:key (europe-record))))    
      (is (= (.getKind key) (entity-kind Country)))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de")))
    (is (= (:iso-3166-alpha-2 country) "de"))
    (is (= (:kind country) (.getKind (:key country))))
    (is (= (:location country) location))
    (is (= (:name country) "Germany"))))

(datastore-test test-region
  (let [location {:latitude 52.52 :longitude 13.41}
        region (region (make-germany) :country-id "de" :name "Berlin" :location location)]
    (let [key (:key region)]
      (is (key? key))
      (is (.isComplete key))    
      (is (= (.getParent key) (:key (make-germany))))    
      (is (= (.getKind key) (entity-kind Region)))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de-berlin")))
    (is (= (:country-id region) "de"))
    (is (= (:kind region) (.getKind (:key region))))
    (is (= (:location region) location))
    (is (= (:name region) "Berlin"))))


;; (datastore-test test-serialize
;;   (testing "without parent key"
;;     (are [record]
;;       (let [entity (serialize record)]
;;         (is (entity? entity))
;;         (are [property-name property-value]
;;           (is (= (.getProperty entity (stringify property-name)) property-value))
;;           :name "Europe"
;;           :iso-3166-alpha-2 "eu")
;;         (let [key (.getKey entity)]
;;           (is (key? key))
;;           (is (.isComplete key))
;;           (is (nil? (.getParent key)))
;;           (is (= (.getId key) 0))
;;           (is (= (.getName key) "eu"))))
;;       (europe-array-map)
;;       (europe-entity)
;;       (europe-hash-map)
;;       (europe-record))))

(datastore-test test-create  
  (are [object]
    (do
      (is (nil? (lookup object)))
      (let [entity (create object)]
        ;; (is (continent entity))        
        ;; (is (= (lookup entity) entity))
        ;; (is (thrown? Exception (create object)))
        (is (delete entity))
        )
      )
    (europe-array-map)
    (europe-entity)
    (europe-hash-map)
    (europe-record)))

(datastore-test test-delete  
  (are [record]
    (do
      (create record)
      (is (not (nil? (lookup record))))
      (is (delete record))
      (is (nil? (lookup record)))
      (is (delete record)))
    (europe-array-map)
    (europe-entity)
    (europe-hash-map)
    (europe-record)))

(datastore-test test-save
  (are [object]        
    (let [entity (save object)]
      (is (continent? entity))        
      (is (= (lookup entity) entity))
      (is (map? (save object)))
      (is (delete entity)))
    (europe-array-map)
    (europe-entity)
    (europe-hash-map)
    (europe-record)))

(datastore-test test-lookup
  (are [object]        
    (let [entity (save object)]
      (is (continent? (lookup entity)))
      (is (= (lookup entity) entity))
      (is (delete entity))
      (is (nil? (lookup entity))))
    (europe-array-map)
    (europe-entity)
    (europe-hash-map)
    (europe-record)))

(datastore-test test-update
  (let [updates {:name "Asia"}]
    (are [object key-vals]
     (do
       (is (delete object))
       (let [entity (update object key-vals)]
         (doseq [[key value] key-vals]
           (is (= (key entity) value)))
         (is (map? entity))        
         (is (= (select-keys entity (keys key-vals)) key-vals))        
         (is (= (update object key-vals) entity))))
     (europe-array-map) updates
     (europe-entity) updates
     (europe-hash-map) updates
     (europe-record) updates)))

(datastore-test test-update-with-location
  (let [updates {:name "Asia" :location {:latitude 1 :longitude 2}}]
    (are [object key-vals]
      (do
        (is (delete object))
        (let [entity (update object key-vals)]
          (doseq [[key value] key-vals]
            (is (= (key entity) value)))
          (is (map? entity))        
          (is (= (select-keys entity (keys key-vals)) key-vals))        
          (is (= (update object key-vals) entity))))
      (europe-array-map) updates
      (europe-entity) updates
      (europe-hash-map) updates
      (europe-record) updates)))

;; (with-local-datastore
;;   (update (europe-entity) {:name "Asia" :location {:latitude 1 :longitude 2}}))

;; (with-local-datastore
;;   (update (europe-entity) {:name "Asia"}))

;; (with-local-datastore
;;   (let [continent (create (europe-record))]
;;     (appengine.datastore.query/select "appengine.datastore.test.entities.Continent" where (= :iso-3166-alpha-2 "eu"))
;;     ))
















;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; OLD


;; (datastore-test test-entity-key-fn-with-person
;;   (let [key-fn (entity-key-fn nil Person)]
;;     (is (fn? key-fn))
;;     (is (nil? (key-fn :name "Bob")))))

;; (datastore-test test-entity-key-fn-with-continent
;;   (let [key-fn (entity-key-fn nil Continent :iso-3166-alpha-2 #'lower-case)]
;;     (is (fn? key-fn))
;;     (let [key (key-fn :iso-3166-alpha-2 "EU")]
;;       (is (key? key))
;;       (is (= (.getKind key) (entity-kind Continent)))
;;       (is (nil? (.getParent key)))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "eu"))
;;       (is (.isComplete key)))))

;; (datastore-test test-entity-key-fn-with-country
;;   (let [key-fn (entity-key-fn Continent Country :iso-3166-alpha-2 #'lower-case)]
;;     (is (fn? key-fn))
;;     (let [key (key-fn (europe-record) :iso-3166-alpha-2 "DE")]
;;       (is (key? key))
;;       (is (= (.getKind key) (entity-kind Country)))
;;       (is (= (.getParent key) (:key (europe-record))))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "de"))
;;       (is (.isComplete key)))))

;; (datastore-test test-entity-key-fn-with-region
;;   (let [key-fn (entity-key-fn Country Region :iso-3166-alpha-2 #'lower-case :name #'lower-case)]
;;     (is (fn? key-fn))
;;     (let [key (key-fn (make-germany) :iso-3166-alpha-2 "DE" :name "Berlin")]
;;       (is (key? key))
;;       (is (= (.getKind key) (entity-kind Region)))
;;       (is (= (.getParent key) (:key (make-germany))))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "de-berlin"))
;;       (is (.isComplete key)))))

;; (datastore-test test-entity-fn-with-person
;;   (entity-fn nil Person person-key :name)
;;   (let [entity-fn (entity-fn nil Person person-key :name)]
;;     (is (fn? entity-fn))
;;     (let [person (entity-fn :name "Bob")]
;;       (is (nil? (:key person)))
;;       (is (= (:name person) "Bob")))))

;; (datastore-test test-entity-fn-with-continent
;;   (let [entity-fn (entity-fn nil Continent continent-key :iso-3166-alpha-2 :location :name)]
;;     (is (fn? entity-fn))
;;     (let [location {:latitude 54.52 :longitude 15.25}
;;           continent (entity-fn :iso-3166-alpha-2 "EU" :location location :name "Europe")]
;;       (is (continent? continent))
;;       (let [key (:key continent)]
;;         (is (key? key))
;;         (is (= (.getKind key) (entity-kind Continent)))
;;         (is (nil? (.getParent key)))
;;         (is (= (.getId key) 0))
;;         (is (= (.getName key) "eu"))
;;         (is (.isComplete key)))
;;       (is (= (:name continent) "Europe"))
;;       (is (= (:iso-3166-alpha-2 continent) "EU"))
;;       (is (= (:location continent) location)))))

;; (datastore-test test-entity-fn-with-country
;;   (let [entity-fn (entity-fn Continent Country country-key :iso-3166-alpha-2 :location :name)]
;;     (is (fn? entity-fn))
;;     (let [location {:latitude 51.16 :longitude 10.45}
;;           country (entity-fn (europe-record) :iso-3166-alpha-2 "DE" :location location :name "Germany")]
;;       (is (country? country))
;;       (let [key (:key country)]
;;         (is (key? key))
;;         (is (= (.getKind key) (entity-kind Country)))
;;         (is (= (.getParent key) (:key (europe-record))))
;;         (is (= (.getId key) 0))
;;         (is (= (.getName key) "de"))
;;         (is (.isComplete key)))
;;       (is (= (:name country) "Germany"))
;;       (is (= (:iso-3166-alpha-2 country) "DE"))
;;       (is (= (:location country) location)))))

;; (datastore-test test-entity-fn-with-region
;;   (let [entity-fn (entity-fn Country Region region-key :country-id :location :name)]
;;     (is (fn? entity-fn))
;;     (let [location {:latitude 52.52 :longitude 13.41}
;;           region (entity-fn (make-germany) :country-id "DE" :name "Berlin" :location location)]
;;       (is (region? region))
;;       (let [key (:key region)]
;;         (is (key? key))
;;         (is (= (.getKind key) (entity-kind Region)))
;;         (is (= (.getParent key) (:key (make-germany))))
;;         (is (= (.getId key) 0))
;;         (is (= (.getName key) "de-berlin"))
;;         (is (.isComplete key)))
;;       (is (= (:country-id region) "DE"))
;;       (is (= (:name region) "Berlin"))
;;       (is (= (:location region) location)))))


;; ;; (datastore-test test-record-with-entity
;; ;;   (let [europe (record (europe-entity))]
;; ;;     (is (continent? europe))
;; ;;     (let [key (:key europe)]
;; ;;       (is (key? key))
;; ;;       (is (= key (:key (europe-hash-map)))))
;; ;;     (is (isa? (class (:kind europe)) String))
;; ;;     (is (= (:kind europe) (:kind (europe-hash-map))))))

;; ;; (datastore-test test-record-with-hash-map
;; ;;   (let [europe (record (europe-hash-map))]
;; ;;     (is (continent? europe))
;; ;;     (let [key (:key europe)]
;; ;;       (is (key? key))
;; ;;       (is (= key (:key (europe-hash-map)))))
;; ;;     (is (isa? (class (:kind europe)) String))
;; ;;     (is (= (:kind europe) (:kind (europe-hash-map))))))

;; ;; (datastore-test test-record-with-resolveable  
;; ;;   (are [kind]
;; ;;     (is (continent? (record kind)))
;; ;;     Continent
;; ;;     'appengine.datastore.test.entities.Continent
;; ;;     (pr-str Continent)
;; ;;     (continent-key :iso-3166-alpha-2 "eu")
;; ;;     (entity-kind Continent)))

;; (datastore-test test-record-with-unresolveable  
;;   (are [kind]
;;     (is (nil? (record kind)))
;;     nil "" "UNRESOLVEABLE-RECORD-CLASS" 'UNRESOLVEABLE-RECORD-SYM))

;; (datastore-test test-deserialize-fn-with-person
;;   (let [record ((deserialize-fn) (person :name "Bob"))]
;;     (is (person? record))
;;     (is (nil? (:key record)))
;;     (is (= (:kind record) (entity-kind Person)))
;;     (are [property-key value]
;;       (is (= (property-key record) value))
;;       :name "Bob")))


;; (datastore-test test-deserialize-fn-with-continent
;;   (let [europe (europe-record)
;;         location (GeoPt. (:latitude (:location europe)) (:longitude (:location europe)))
;;         record ((deserialize-fn :location GeoPt) (assoc europe :location location))]
;;     (is (continent? record))
;;     (let [key (:key record)]
;;       (is (key? key))
;;       (is (.isComplete key))
;;       (is (nil? (.getParent key)))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "eu")))
;;     (are [property-key value]
;;       (is (= (property-key record) value))
;;       :name (:name europe)
;;       :kind (:kind europe)
;;       :iso-3166-alpha-2 (:iso-3166-alpha-2 europe)
;;       :location (:location europe))))

;; (datastore-test test-serialize-fn-with-person
;;   (let [record (person :name "Bob")
;;         entity ((serialize-fn) record)]
;;     (is (entity? entity))
;;     (is (= (.getKind entity) "person"))
;;     (let [key (.getKey entity)]
;;       (is (key? key))
;;       (is (not (.isComplete key)))
;;       (is (nil? (.getParent key)))
;;       (is (= (.getId key) 0))
;;       (is (nil? (.getName key))))
;;     (are [property-key value]
;;       (is (= (.getProperty entity (name property-key)) value))
;;       :name "Bob")))

;; (datastore-test test-serialize-fn-with-person
;;   (let [record (person :name "Bob")
;;         entity ((serialize-fn) record)]
;;     (is (entity? entity))
;;     (is (= (.getKind entity) (entity-kind Person)))
;;     (let [key (.getKey entity)]
;;       (is (key? key))
;;       (is (not (.isComplete key)))
;;       (is (nil? (.getParent key)))
;;       (is (= (.getId key) 0))
;;       (is (nil? (.getName key))))
;;     (are [property-key value]
;;       (is (= (.getProperty entity (name property-key)) value))
;;       :name "Bob")))

;; (datastore-test test-serialize-fn-with-continent
;;   (let [record (europe-record)
;;         entity ((serialize-fn :location GeoPt) record)]
;;     (is (entity? entity))
;;     (is (= (.getKind entity) (entity-kind Continent)))
;;     (let [key (.getKey entity)]
;;       (is (key? key))
;;       (is (.isComplete key))
;;       (is (nil? (.getParent key)))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "eu")))
;;     (are [property-key value]
;;       (is (= (.getProperty entity (name property-key)) value))
;;       :name "Europe"
;;       :iso-3166-alpha-2 "eu"
;;       :location (GeoPt. (:latitude (:location record)) (:longitude (:location record))))))

;; (datastore-test test-serialize-fn-with-country
;;   (let [record (make-germany)
;;         entity ((serialize-fn :location GeoPt) record)]
;;     (is (entity? entity))
;;     (is (= (.getKind entity) (entity-kind Country)))
;;     (let [key (.getKey entity)]
;;       (is (key? key))
;;       (is (.isComplete key))
;;       (is (= (.getParent key) (:key (europe-record))))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "de")))
;;     (are [property-key value]
;;       (is (= (.getProperty entity (name property-key)) value))
;;       :name "Germany"
;;       :iso-3166-alpha-2 "de"
;;       :location (GeoPt. (:latitude (:location record)) (:longitude (:location record))))))

;; (datastore-test test-serialize-fn-with-region
;;   (let [record (make-berlin)
;;         entity ((serialize-fn :location GeoPt) record)]
;;     (is (entity? entity))
;;     (is (= (.getKind entity) (entity-kind Region)))
;;     (let [key (.getKey entity)]
;;       (is (key? key))
;;       (is (.isComplete key))
;;       (is (= (.getParent key) (:key (make-germany))))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "de-berlin")))
;;     (are [property-key value]
;;       (is (= (.getProperty entity (name property-key)) value))
;;       :name "Berlin"
;;       :country-id "de"
;;       :location (GeoPt. (:latitude (:location record)) (:longitude (:location record))))))
