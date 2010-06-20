(ns appengine.datastore.test.entities
  (:import (com.google.appengine.api.datastore Entity Key GeoPt))
  (:use appengine.datastore.entities
        appengine.datastore.keys
        appengine.datastore.protocols
        appengine.test
        appengine.utils
        clojure.test
        [clojure.contrib.string :only (join lower-case)]))

(refer-private 'appengine.datastore.entities)

(defentity Person ()
  ((name)))

(defentity Continent ()
  ((iso-3166-alpha-2 :key lower-case)
   (location :serialize GeoPt)
   (name)))

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
  (serialize (apply continent (europe-seq))))

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

(datastore-test test-blank-entity-with-key  
  (let [entity (blank-entity (make-key (entity-kind Continent) 1))]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) (entity-kind Continent)))
    (is (nil? (.getParent entity)))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (isa? (class key) Key))
      (is (= (.getKind key) (entity-kind Continent)))
      (is (nil? (.getParent key)))
      (is (= (.getId key) 1))
      (is (nil? (.getName key)))
      (is (.isComplete key)))))

(datastore-test test-blank-entity-with-kind
  (let [entity (blank-entity (entity-kind Continent))]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) (entity-kind Continent)))
    (is (nil? (.getParent entity)))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (isa? (class key) Key))
      (is (= (.getKind key) (entity-kind Continent)))
      (is (nil? (.getParent key)))
      (is (= (.getId key) 0))
      (is (nil? (.getName key)))
      (is (not (.isComplete key))))))

(datastore-test test-blank-entity-with-key
  (let [key (make-key (entity-kind Continent) "eu") entity (blank-entity key)]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) (entity-kind Continent)))
    (is (nil? (.getParent entity)))
    (is (empty? (.getProperties entity)))
    (is (= (.getKey entity) key))))

(datastore-test test-record-with-entity
  (let [europe (record (europe-entity))]
    (is (continent? europe))
    (let [key (:key europe)]
      (is (key? key))
      (is (= key (:key (europe-hash-map)))))
    (is (isa? (class (:kind europe)) String))
    (is (= (:kind europe) (:kind (europe-hash-map))))))

(datastore-test test-record-with-hash-map
  (let [europe (record (europe-hash-map))]
    (is (continent? europe))
    (let [key (:key europe)]
      (is (key? key))
      (is (= key (:key (europe-hash-map)))))
    (is (isa? (class (:kind europe)) String))
    (is (= (:kind europe) (:kind (europe-hash-map))))))

(datastore-test test-record-with-resolveable  
  (are [kind]
    (is (continent? (record kind)))
    Continent
    'appengine.datastore.test.entities.Continent
    (pr-str Continent)
    (continent-key :iso-3166-alpha-2 "eu")
    (entity-kind Continent)))

(datastore-test test-record-with-unresolveable  
  (are [kind]
    (is (nil? (record kind)))
    nil "" "UNRESOLVEABLE-RECORD-CLASS" 'UNRESOLVEABLE-RECORD-SYM))

(datastore-test test-entity?
  (is (not (entity? nil)))
  (is (not (entity? "")))
  (is (entity? (Entity. "person"))))

(deftest test-entity?-fn-name
  (are [record name]
    (is (= (entity?-fn-name record) name))
    'Continent 'continent?
    'CountryFlag 'country-flag?))

(deftest test-entity-kind
  (are [entity kind]
    (is (= (entity-kind entity) kind))
    nil nil
    Continent "appengine.datastore.test.entities.Continent"))

(deftest test-find-entities-fn-name
  (are [record name]
    (is (= (find-entities-fn-name record) name))
    'Continent 'find-continents
    'CountryFlag 'find-country-flags))

(deftest test-key-fn-name
  (are [record name]
    (is (= (key-fn-name record) name))
    'Continent 'continent-key
    'CountryFlag 'country-flag-key))

(deftest test-entity-fn-name
  (are [record name]
    (is (= (entity-fn-name record) name))
    Continent 'continent
    'Continent 'continent
    'CountryFlag 'country-flag))

(datastore-test test-blank-entity-with-parent-kind-and-named-key
  (let [parent-key (make-key (entity-kind Continent) "eu")
        entity (blank-entity parent-key (entity-kind Country) "de")]
    (is (isa? (class entity) Entity))
    (is (= (.getKind entity) (entity-kind Country)))
    (is (= (.getParent entity) parent-key))
    (is (empty? (.getProperties entity)))
    (let [key (.getKey entity)]
      (is (isa? (class key) Key))
      (is (= (.getKind key) (entity-kind Country)))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de"))
      (is (.isComplete key))
      (is (= (.getParent key) parent-key)))))

(datastore-test test-continent?
  (is (not (continent? nil)))
  (is (not (continent? "")))
  (let [continent (europe-record)]
    (is (continent? continent))
    (is (not (continent? (make-germany))))))

(deftest test-extract-deserializer
  (is (= (extract-deserializer continent-specification)
         [:location 'GeoPt])))

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
         [:location 'GeoPt])))

;; (datastore-test test-find-continents
;;   (let [europe (save (europe-record))
;;         continents (find-continents)]
;;     (is (seq? continents))
;;     (is (not (empty? continents)))
;;     (is (= (first continents) europe))))

(datastore-test test-entity-key-fn-with-person
  (let [key-fn (entity-key-fn nil Person)]
    (is (fn? key-fn))
    (is (nil? (key-fn :name "Bob")))))

(datastore-test test-entity-key-fn-with-continent
  (let [key-fn (entity-key-fn nil Continent :iso-3166-alpha-2 #'lower-case)]
    (is (fn? key-fn))
    (let [key (key-fn :iso-3166-alpha-2 "EU")]
      (is (key? key))
      (is (= (.getKind key) (entity-kind Continent)))
      (is (nil? (.getParent key)))
      (is (= (.getId key) 0))
      (is (= (.getName key) "eu"))
      (is (.isComplete key)))))

(datastore-test test-entity-key-fn-with-country
  (let [key-fn (entity-key-fn Continent Country :iso-3166-alpha-2 #'lower-case)]
    (is (fn? key-fn))
    (let [key (key-fn (europe-record) :iso-3166-alpha-2 "DE")]
      (is (key? key))
      (is (= (.getKind key) (entity-kind Country)))
      (is (= (.getParent key) (:key (europe-record))))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de"))
      (is (.isComplete key)))))

(datastore-test test-entity-key-fn-with-region
  (let [key-fn (entity-key-fn Country Region :iso-3166-alpha-2 #'lower-case :name #'lower-case)]
    (is (fn? key-fn))
    (let [key (key-fn (make-germany) :iso-3166-alpha-2 "DE" :name "Berlin")]
      (is (key? key))
      (is (= (.getKind key) (entity-kind Region)))
      (is (= (.getParent key) (:key (make-germany))))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de-berlin"))
      (is (.isComplete key)))))

(datastore-test test-entity-fn-with-person
  (entity-fn nil Person person-key :name)
  (let [entity-fn (entity-fn nil Person person-key :name)]
    (is (fn? entity-fn))
    (let [person (entity-fn :name "Bob")]
      (is (nil? (:key person)))
      (is (= (:name person) "Bob")))))

(datastore-test test-entity-fn-with-continent
  (let [entity-fn (entity-fn nil Continent continent-key :iso-3166-alpha-2 :location :name)]
    (is (fn? entity-fn))
    (let [location {:latitude 54.52 :longitude 15.25}
          continent (entity-fn :iso-3166-alpha-2 "EU" :location location :name "Europe")]
      (is (continent? continent))
      (let [key (:key continent)]
        (is (key? key))
        (is (= (.getKind key) (entity-kind Continent)))
        (is (nil? (.getParent key)))
        (is (= (.getId key) 0))
        (is (= (.getName key) "eu"))
        (is (.isComplete key)))
      (is (= (:name continent) "Europe"))
      (is (= (:iso-3166-alpha-2 continent) "EU"))
      (is (= (:location continent) location)))))

(datastore-test test-entity-fn-with-country
  (let [entity-fn (entity-fn Continent Country country-key :iso-3166-alpha-2 :location :name)]
    (is (fn? entity-fn))
    (let [location {:latitude 51.16 :longitude 10.45}
          country (entity-fn (europe-record) :iso-3166-alpha-2 "DE" :location location :name "Germany")]
      (is (country? country))
      (let [key (:key country)]
        (is (key? key))
        (is (= (.getKind key) (entity-kind Country)))
        (is (= (.getParent key) (:key (europe-record))))
        (is (= (.getId key) 0))
        (is (= (.getName key) "de"))
        (is (.isComplete key)))
      (is (= (:name country) "Germany"))
      (is (= (:iso-3166-alpha-2 country) "DE"))
      (is (= (:location country) location)))))

(datastore-test test-entity-fn-with-region
  (let [entity-fn (entity-fn Country Region region-key :country-id :location :name)]
    (is (fn? entity-fn))
    (let [location {:latitude 52.52 :longitude 13.41}
          region (entity-fn (make-germany) :country-id "DE" :name "Berlin" :location location)]
      (is (region? region))
      (let [key (:key region)]
        (is (key? key))
        (is (= (.getKind key) (entity-kind Region)))
        (is (= (.getParent key) (:key (make-germany))))
        (is (= (.getId key) 0))
        (is (= (.getName key) "de-berlin"))
        (is (.isComplete key)))
      (is (= (:country-id region) "DE"))
      (is (= (:name region) "Berlin"))
      (is (= (:location region) location)))))

(datastore-test test-continent-key
  (let [key (continent-key :iso-3166-alpha-2 "eu")]
    (is (key? key))
    (is (.isComplete key))
    (is (nil? (.getParent key)))    
    (is (= (.getKind key) (entity-kind Continent)))
    (is (= (.getId key) 0))
    (is (= (.getName key) "eu"))))

(datastore-test test-country-key
  (let [continent-key (make-key (entity-kind Continent) "eu")
        key (country-key continent-key :iso-3166-alpha-2 "de")]
    (is (key? key))
    (is (.isComplete key))    
    (is (= (.getParent key) continent-key))    
    (is (= (.getKind key) (entity-kind Country)))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de"))))

(datastore-test test-region-key
  (let [continent-key (make-key (entity-kind Continent) "eu")
        country-key (country-key continent-key :iso-3166-alpha-2 "de")
        key (region-key country-key :country-id "de" :name "Berlin")]
    (is (key? key))
    (is (.isComplete key))    
    (is (= (.getParent key) country-key))    
    (is (= (.getParent (.getParent key)) continent-key))    
    (is (= (.getKind key) (entity-kind Region)))
    (is (= (.getId key) 0))
    (is (= (.getName key) "de-berlin"))))

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

(datastore-test test-deserialize-fn-with-person
  (let [record ((deserialize-fn) (person :name "Bob"))]
    (is (person? record))
    (is (nil? (:key record)))
    (is (= (:kind record) (entity-kind Person)))
    (are [property-key value]
      (is (= (property-key record) value))
      :name "Bob")))


(datastore-test test-deserialize-fn-with-continent
  (let [europe (europe-record)
        location (GeoPt. (:latitude (:location europe)) (:longitude (:location europe)))
        record ((deserialize-fn :location GeoPt) (assoc europe :location location))]
    (is (continent? record))
    (let [key (:key record)]
      (is (key? key))
      (is (.isComplete key))
      (is (nil? (.getParent key)))
      (is (= (.getId key) 0))
      (is (= (.getName key) "eu")))
    (are [property-key value]
      (is (= (property-key record) value))
      :name (:name europe)
      :kind (:kind europe)
      :iso-3166-alpha-2 (:iso-3166-alpha-2 europe)
      :location (:location europe))))

(datastore-test test-serialize-fn-with-person
  (let [record (person :name "Bob")
        entity ((serialize-fn) record)]
    (is (entity? entity))
    (is (= (.getKind entity) "person"))
    (let [key (.getKey entity)]
      (is (key? key))
      (is (not (.isComplete key)))
      (is (nil? (.getParent key)))
      (is (= (.getId key) 0))
      (is (nil? (.getName key))))
    (are [property-key value]
      (is (= (.getProperty entity (name property-key)) value))
      :name "Bob")))

(datastore-test test-serialize-fn-with-person
  (let [record (person :name "Bob")
        entity ((serialize-fn) record)]
    (is (entity? entity))
    (is (= (.getKind entity) (entity-kind Person)))
    (let [key (.getKey entity)]
      (is (key? key))
      (is (not (.isComplete key)))
      (is (nil? (.getParent key)))
      (is (= (.getId key) 0))
      (is (nil? (.getName key))))
    (are [property-key value]
      (is (= (.getProperty entity (name property-key)) value))
      :name "Bob")))

(datastore-test test-serialize-fn-with-continent
  (let [record (europe-record)
        entity ((serialize-fn :location GeoPt) record)]
    (is (entity? entity))
    (is (= (.getKind entity) (entity-kind Continent)))
    (let [key (.getKey entity)]
      (is (key? key))
      (is (.isComplete key))
      (is (nil? (.getParent key)))
      (is (= (.getId key) 0))
      (is (= (.getName key) "eu")))
    (are [property-key value]
      (is (= (.getProperty entity (name property-key)) value))
      :name "Europe"
      :iso-3166-alpha-2 "eu"
      :location (GeoPt. (:latitude (:location record)) (:longitude (:location record))))))

(datastore-test test-serialize-fn-with-country
  (let [record (make-germany)
        entity ((serialize-fn :location GeoPt) record)]
    (is (entity? entity))
    (is (= (.getKind entity) (entity-kind Country)))
    (let [key (.getKey entity)]
      (is (key? key))
      (is (.isComplete key))
      (is (= (.getParent key) (:key (europe-record))))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de")))
    (are [property-key value]
      (is (= (.getProperty entity (name property-key)) value))
      :name "Germany"
      :iso-3166-alpha-2 "de"
      :location (GeoPt. (:latitude (:location record)) (:longitude (:location record))))))

(datastore-test test-serialize-fn-with-region
  (let [record (make-berlin)
        entity ((serialize-fn :location GeoPt) record)]
    (is (entity? entity))
    (is (= (.getKind entity) (entity-kind Region)))
    (let [key (.getKey entity)]
      (is (key? key))
      (is (.isComplete key))
      (is (= (.getParent key) (:key (make-germany))))
      (is (= (.getId key) 0))
      (is (= (.getName key) "de-berlin")))
    (are [property-key value]
      (is (= (.getProperty entity (name property-key)) value))
      :name "Berlin"
      :country-id "de"
      :location (GeoPt. (:latitude (:location record)) (:longitude (:location record))))))

(datastore-test test-serialize
  (testing "without parent key"
    (are [record]
      (let [entity (serialize record)]
        (is (entity? entity))
        (are [property-name property-value]
          (is (= (.getProperty entity (stringify property-name)) property-value))
          :name "Europe"
          :iso-3166-alpha-2 "eu")
        (let [key (.getKey entity)]
          (is (key? key))
          (is (.isComplete key))
          (is (nil? (.getParent key)))
          (is (= (.getId key) 0))
          (is (= (.getName key) "eu"))))
      (europe-array-map)
      (europe-entity)
      (europe-hash-map)
      (europe-record))))

(datastore-test test-create  
  (are [object]
    (do
      (is (nil? (lookup object)))
      (let [entity (create object)]
        (is (map? entity))        
        (is (= (lookup entity) entity))
        (is (thrown? Exception (create object)))
        (is (delete entity))))
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
      (is (delete entity))
      )
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
;;   (update (europe-record) {:name "Asia"}))
