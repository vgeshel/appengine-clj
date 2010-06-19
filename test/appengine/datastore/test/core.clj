(ns appengine.datastore.test.core
  (:use clojure.test
        appengine.datastore.core
        appengine.datastore.keys
        appengine.test)
  (:import (com.google.appengine.api.datastore
            DatastoreServiceFactory
            Entity
            Key
            EntityNotFoundException
            KeyFactory
            Query
            Query$FilterOperator
            Query$SortDirection)))

;; (defn make-europe []
;;   (doto (Entity. (make-key "continent" "eu"))
;;     (.setProperty "name" "Europe")))

;; (defn create-example-entity []
;;   (save (make-europe)))


;; (datastore-test test-make-key-with-int
;;   (let [key (make-key "person" 1)]
;;     (is (= (class key) com.google.appengine.api.datastore.Key))
;;     (is (.isComplete key))
;;     (is (nil? (.getParent key)))    
;;     (is (= (.getKind key) "person"))
;;     (is (= (.getId key) 1))
;;     (is (nil? (.getName key)))))

;; (datastore-test test-make-key-with-string
;;   (let [key (make-key "country" "de")]
;;     (is (= (class key) com.google.appengine.api.datastore.Key))
;;     (is (.isComplete key))
;;     (is (nil? (.getParent key)))
;;     (is (= (.getKind key) "country"))
;;     (is (= (.getId key) 0))
;;     (is (= (.getName key) "de"))))

;; (datastore-test test-make-key-with-parent
;;   (let [continent (make-key "continent" "eu")
;;         country (make-key continent "country" "de")]
;;     (is (= (class country) com.google.appengine.api.datastore.Key))
;;     (is (.isComplete country))
;;     (is (= (.getParent country) continent))
;;     (is (= (.getKind country) "country"))
;;     (is (= (.getId country) 0))
;;     (is (= (.getName country) "de"))))

;; (datastore-test test-entity->map
;;   (let [continent (entity->map (doto (Entity. "continent") (.setProperty "name" "Europe")))]
;;     (let [key (:key continent)]
;;       (is (not (.isComplete key)))
;;       (is (nil? (.getParent key)))
;;       (is (= (.getKind key) "continent"))
;;       (is (= (.getId key) 0))
;;       (is (nil? (.getName key)))
;;       (is (= (class key) com.google.appengine.api.datastore.Key)))
;;     (is (= (:kind continent) "continent"))
;;     (is (= (:name continent) "Europe"))))

;; (datastore-test test-entity->map-with-key
;;   (let [continent (entity->map (doto (Entity. (make-key "continent" "eu")) (.setProperty "name" "Europe")))]
;;     (let [key (:key continent)]
;;       (is (.isComplete key))
;;       (is (nil? (.getParent key)))
;;       (is (= (.getKind key) "continent"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "eu"))
;;       (is (= (class key) com.google.appengine.api.datastore.Key)))
;;     (is (= (:kind continent) "continent"))
;;     (is (= (:name continent) "Europe"))))

;; (datastore-test test-entity->map-with-parent
;;   (let [continent (entity->map (doto (Entity. (make-key "continent" "eu")) (.setProperty "name" "Europe")))
;;         country (entity->map (doto (Entity. (make-key (:key continent) "country" "es")) (.setProperty "name" "Spain")))]    
;;     (let [key (:key country)]
;;       (is (.isComplete key))
;;       (is (= (.getParent key) (:key continent)))
;;       (is (= (.getKind key) "country"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "es"))
;;       (is (= (class key) com.google.appengine.api.datastore.Key)))
;;     (is (= (:kind country) "country"))
;;     (is (= (:name country) "Spain"))))

;; (datastore-test test-entity->map-with-exisiting-entity
;;   (let [continent (entity->map (map->entity (create-entity {:key (make-key "continent" "eu") :name "Europe"})))]
;;     (let [key (:key continent)]
;;       (is (.isComplete key))
;;       (is (nil? (.getParent key)))
;;       (is (= (.getKind key) "continent"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "eu"))
;;       (is (= (class key) com.google.appengine.api.datastore.Key)))
;;     (is (= (:kind continent) "continent"))
;;     (is (= (:name continent) "Europe"))))

;; (datastore-test test-entity->map-with-exisiting-entity-and-parent
;;   (let [continent (create-entity {:key (make-key "continent" "eu") :name "Europe"})
;;         country (entity->map (map->entity (create-entity {:key (make-key (:key continent) "country" "es") :name "Spain"})))]
;;     (let [key (:key country)]
;;       (is (.isComplete key))
;;       (is (= (.getParent key) (:key continent)))
;;       (is (= (.getKind key) "country"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "es"))
;;       (is (= (class key) com.google.appengine.api.datastore.Key)))
;;     (is (= (:kind continent) "continent"))
;;     (is (= (:name continent) "Europe"))))

;; (datastore-test test-key->string  
;;   (is (= (key->string (make-key "person" 1)) "agR0ZXN0cgwLEgZwZXJzb24YAQw"))
;;   (is (= (key->string (make-key "country" "de")) "agR0ZXN0cg8LEgdjb3VudHJ5IgJkZQw"))
;;   (let [continent (make-key "continent" "eu")
;;         country (make-key continent "country" "de")]
;;     (is (= (key->string country) "agR0ZXN0ciALEgljb250aW5lbnQiAmV1DAsSB2NvdW50cnkiAmRlDA"))))

;; (datastore-test test-string->key
;;   (let [key (string->key "agR0ZXN0cgwLEgZwZXJzb24YAQw")]
;;     (is (= (.getKind key) "person"))
;;     (is (= (.getId key) (long 1)))
;;     (is (nil? (.getName key))))
;;   (let [key (string->key "agR0ZXN0cg8LEgdjb3VudHJ5IgJkZQw")]
;;     (is (= (.getKind key) "country"))
;;     (is (= (.getId key) 0))
;;     (is (= (.getName key) "de")))
;;   (let [key (string->key "agR0ZXN0ciALEgljb250aW5lbnQiAmV1DAsSB2NvdW50cnkiAmRlDA")]
;;     (is (= (.getKind key) "country"))
;;     (is (= (.getId key) 0))
;;     (is (= (.getName key) "de"))
;;     (let [parent (.getParent key)]
;;       (is (= (.getKind parent) "continent"))
;;       (is (= (.getId parent) 0))
;;       (is (= (.getName parent) "eu")))))

;; (datastore-test test-map->entity-with-string-keys
;;   (let [entity (map->entity {"kind" "continent" "name" "Europe"})]
;;     (is (= (class entity) Entity))
;;     (is (= (.getKind entity) "continent"))
;;     (is (= (.. entity getKey getKind) "continent"))
;;     (is (= (.. entity getKey getId) 0))
;;     (is (nil? (.. entity getKey getName)))
;;     (is (= (. entity getProperty "name") "Europe"))))

;; (datastore-test test-map->entity-with-kind
;;   (let [entity (map->entity {:kind "continent" :name "Europe"})]
;;     (is (= (class entity) Entity))
;;     (is (= (.getKind entity) "continent"))
;;     (is (= (.. entity getKey getKind) "continent"))
;;     (is (= (.. entity getKey getId) 0))
;;     (is (nil? (.. entity getKey getName)))
;;     (is (= (. entity getProperty "name") "Europe"))))

;; (datastore-test test-map->entity-with-key
;;   (let [entity (map->entity {:key (make-key "continent" "eu") :name "Europe"})]
;;     (is (= (class entity) Entity))
;;     (is (= (.getKind entity) "continent"))
;;     (is (= (.. entity getKey getKind) "continent"))
;;     (is (= (.. entity getKey getId) 0))
;;     (is (= (.. entity getKey getName) "eu"))
;;     (is (= (. entity getProperty "name") "Europe"))))

;; (datastore-test test-map->entity-with-existing-entity
;;   (let [continent (create-entity {:key (make-key "continent" "eu") :name "Europe"})
;;         continent (map->entity continent)]
;;     (let [key (.getKey continent)]
;;       (is (.isComplete key))
;;       (is (= (.getParent key) (:key continent)))
;;       (is (= (.getKind key) "continent"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "eu"))
;;       (is (= (class key) com.google.appengine.api.datastore.Key)))
;;     (is (= (.getKind continent) "continent"))
;;     (is (= (. continent getProperty "name") "Europe"))))

;; (datastore-test test-map->entity-with-parent-key
;;   (let [continent (create-entity {:key (make-key "continent" "eu") :name "Europe"})
;;         country (map->entity (create-entity {:parent-key (:key continent) :kind "country" :name "Spain"}))]
;;     (let [key (.getKey country)]
;;       (is (not (.isComplete key)))
;;       (is (= (.getParent key) (:key continent)))
;;       (is (= (.getKind key) "country"))
;;       (is (= (.getId key) 0))
;;       (is (nil? (.getName key)))
;;       (is (= (class key) com.google.appengine.api.datastore.Key)))
;;     (is (= (.getKind country) "country"))
;;     (is (= (. country getProperty "name") "Spain"))))

;; (datastore-test test-map->entity-with-parent-and-key
;;   (let [continent (create-entity {:key (make-key "continent" "eu") :name "Europe"})
;;         country (map->entity (create-entity {:key (make-key (:key continent) "country" "es") :name "Spain"}))]
;;     (let [key (.getKey country)]
;;       (is (.isComplete key))
;;       (is (= (.getParent key) (:key continent)))
;;       (is (= (.getKind key) "country"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "es"))
;;       (is (= (class key) com.google.appengine.api.datastore.Key)))
;;     (is (= (.getKind country) "country"))
;;     (is (= (. country getProperty "name") "Spain"))))

;; (datastore-test test-create-entity-with-struct
;;   (defstruct country :name :kind)
;;   (let [country (create-entity (struct country "Germany" "country"))]
;;     (is (not (nil? (:key country))))
;;     (is (= (:name country) "Germany"))
;;     (is (= (:kind country) "country"))))

;; (datastore-test test-create-entity-with-int-key
;;   (let [person (create-entity {:kind "person" :name "Bob"})]
;;     (is (= (class person) clojure.lang.PersistentArrayMap))
;;     (is (.isComplete (:key person)))
;;     (is (= (.getKind (:key person)) "person"))
;;     (is (= (.getId (:key person)) 1))
;;     (is (nil? (.getName (:key person))))      
;;     (is (= (:kind person)) "person")
;;     (is (= (:name person) "Bob"))))

;; (datastore-test test-create-entity-with-string-key
;;   (let [country (create-entity {:key (make-key "country" "de") :name "Germany"})]
;;     (is (= (class country) clojure.lang.PersistentArrayMap))
;;     (is (.isComplete (:key country)))
;;     (is (= (.getKind (:key country)) "country"))
;;     (is (= (.getId (:key country)) 0))
;;     (is (= (.getName (:key country)) "de"))    
;;     (is (= (:kind country)) "country")
;;     (is (= (:name country) "Germany"))))

;; (datastore-test test-get-entity
;;   (is (nil? (get-entity nil)))
;;   (is (nil? (get-entity {}))))

;; (datastore-test test-get-entity-with-int-key
;;   (let [person (create-entity {:kind "person" :name "Bob"})]
;;     (is (= (class person) clojure.lang.PersistentArrayMap))
;;     (is (= ((get-entity person) person)))
;;     (is (= ((get-entity (:key person)) person)))
;;     (is (= ((get-entity (map->entity person)) person)))))

;; (datastore-test test-get-with-string-key
;;   (let [country (create-entity {:key (make-key "country" "de") :name "Germany"})]
;;     (is (= (class country) clojure.lang.PersistentArrayMap))
;;     (is (= ((get-entity country) country)))
;;     (is (= ((get-entity (:key country)) country)))
;;     (is (= ((get-entity (map->entity country)) country)))))

;; (datastore-test test-put-entity
;;   (let [continent (put-entity {:name "Europe" :key (make-key "continent" "eu")})]
;;     (is (= (count (find-all (Query. "continent"))) 1))
;;     (let [key (:key continent)]
;;       (is (.isComplete key))
;;       (is (= (.getKind key) "continent"))
;;       (is (= (.getId key) 0))
;;       (is (= (.getName key) "eu")))
;;     (is (= (:name continent) "Europe"))
;;     (put-entity continent)
;;     (is (= (count (find-all (Query. "continent"))) 1))
;;     (let [country (put-entity {:name "Germany" :key (make-key (:key continent) "country" "de")})]
;;       (is (= (count (find-all (Query. "country"))) 1))
;;       (let [key (:key country)]
;;         (is (.isComplete key))
;;         (is (= (.getParent key) (:key continent)))
;;         (is (= (.getKind key) "country"))
;;         (is (= (.getId key) 0))
;;         (is (= (.getName key) "de")))
;;       (is (= (:name country) "Germany"))
;;       (put-entity country)
;;       (is (= (count (find-all (Query. "country"))) 1)))))

;; (datastore-test delete-entity-with-key
;;   (let [key (:key (create-entity {:kind "person" :name "Bob"}))]
;;     (delete-entity key)
;;     (is (thrown? EntityNotFoundException (get-entity key)))))

;; (datastore-test delete-entity-with-multiple-keys
;;   (let [key1 (:key (create-entity {:kind "person" :name "Alice"}))
;;         key2 (:key (create-entity {:kind "person" :name "Bob"}))]
;;     (delete-entity [key1 key2])
;;     (are (thrown? EntityNotFoundException (get-entity _1))
;;       key1 key2)))

;; ;; (datastore-test test-update-entity
;; ;;   (let [country (put-entity {:key (make-key "country" "de") :name "Deutschland"})]
;; ;;     (let [country (update-entity country {:name "Germany"})]
;; ;;       (is (= (:name country) "Germany"))
;; ;;       (is (= (count (find-all (Query. "country"))) 1)))
;; ;;     (let [country (update-entity (:key country) {:name "Germany"})]
;; ;;       (is (= (:name country) "Germany"))
;; ;;       (is (= (count (find-all (Query. "country"))) 1)))
;; ;;     (let [country (update-entity (map->entity country) {:name "Germany"})]
;; ;;       (is (= (:name country) "Germany"))
;; ;;       (is (= (count (find-all (Query. "country"))) 1)))))

;; (datastore-test test-update-entity-with-parent
;;   (let [continent (create-entity {:key (make-key "continent" "eu") :name "Europe"})
;;         country (create-entity {:key (make-key (:key continent) "country" "de") :name "Deutschland"})]
;;     (let [country (update-entity country {:name "Germany"})]
;;       (is (= (:name country) "Germany"))
;;       (is (= (count (find-all (Query. "country"))) 1)))))

;; (datastore-test test-properties
;;   (let [record {:key (make-key "person" 1) :name "Bob"}]
;;     (is (= (properties record) {:name "Bob"}))
;;     (is (= (properties (map->entity record)) {:name "Bob"}))))

;; (datastore-test entity-to-map-converts-to-persistent-map
;;   (let [entity (doto (Entity. "MyKind")
;;                  (.setProperty "foo" "Foo")
;;                  (.setProperty "bar" "Bar"))]
;;     (.put (DatastoreServiceFactory/getDatastoreService) entity)
;;     (is (= {:foo "Foo" :bar "Bar" :kind "MyKind" :key (.getKey entity)}
;;            (entity->map entity)))))

;; (datastore-test find-all-runs-given-query
;;   (.put (DatastoreServiceFactory/getDatastoreService)
;;         [(doto (Entity. "A") (.setProperty "code" 1) (.setProperty "name" "jim"))
;;          (doto (Entity. "A") (.setProperty "code" 2) (.setProperty "name" "tim"))
;;          (doto (Entity. "B") (.setProperty "code" 1) (.setProperty "name" "jan"))])
;;   (is (= ["jim" "tim"] (map :name (find-all (doto (Query. "A") (.addSort "code"))))))
;;   (is (= ["tim" "jim"] (map :name (find-all (doto (Query. "A") (.addSort "code" Query$SortDirection/DESCENDING))))))
;;   (is (= ["jim"] (map :name (find-all (doto (Query. "A") (.addFilter "code" Query$FilterOperator/EQUAL 1))))))
;;   (is (= ["jan"] (map :name (find-all (doto (Query. "B") (.addFilter "code" Query$FilterOperator/EQUAL 1)))))))

;; (datastore-test create-saves-and-returns-item-with-a-key
;;   (let [created-item (create-entity {:kind "MyKind" :name "hume" :age 31})]
;;     (is (not (nil? (created-item :key))))
;;     (let [created-entity (.get (DatastoreServiceFactory/getDatastoreService) (created-item :key))]
;;       (is (= "MyKind" (.getKind created-entity)))
;;       (is (= "hume" (.getProperty created-entity "name")))
;;       (is (= 31 (.getProperty created-entity "age"))))))

;; (datastore-test get-given-a-key-returns-a-mapified-entity
;;   (let [key (:key (create-entity {:kind "Person" :name "cliff"}))]
;;     (is (= "cliff" ((get-entity key) :name)))))

;; (datastore-test get-multiple-keys-from-ds
;;   (let [e1 (create-entity {:kind "E" :name "e1" })
;; 	e2 (create-entity {:kind "E" :name "e2" })
;; 	e3 (create-entity {:kind "E" :name "e3" })
;; 	entities (get-entity (map :key [e1 e2 e3]))]
;;     (is (= (get entities (:key e1)) e1))
;;     (is (= (get entities (:key e2)) e2))
;;     (is (= (get entities (:key e3)) e3))
;;     (delete-entity (map :key [e1 e2 e3]))
;;     (let [entities (get-entity (map :key [e1 e2 e3]))]
;;       (is (= 0 (reduce count 0 entities))))))

;; (datastore-test update-remove-attribute
;;   (let [e (create-entity {:kind "E" :a "a" :b "b" :c "c"})
;; 	e-updated (update-entity e {:c nil})
;; 	e-updated2 (update-entity e {:c :remove})]
;;     (is (contains? e :c))
;;     (is (contains? e-updated :c))
;;     (is (not (contains? e-updated2 :c)))))

;; (datastore-test delete-entity-multimethod
;;   (let [key (:key (create-entity {:kind "E" :a "a"}))
;; 	entity-as-map (create-entity {:kind "E" :b "b"})
;; 	entity-as-entity (map->entity (create-entity {:kind "E" :c "c"}))
;; 	e1 (create-entity {:kind "E" :name "e1"})
;; 	e2 (create-entity {:kind "E" :name "e2"})]
;;     (delete-entity key)
;;     (delete-entity entity-as-map)
;;     (delete-entity entity-as-entity)
;;     (delete-entity (map :key [e1 e2]))
;;     (are [record] (thrown? EntityNotFoundException (get-entity record))
;; 	 key entity-as-map entity-as-entity e1 e2)))

;; (datastore-test delete-entity-multimethod-with-multiple-deletes
;;   (let [e1 (create-entity {:kind "E" :name "e1"})
;; 	e2 (create-entity {:kind "E" :name "e2"})
;; 	e3 (create-entity {:kind "E" :name "e3"})
;; 	e4 (create-entity {:kind "E" :name "e4"})
;; 	e5 (create-entity {:kind "E" :name "e5"})
;; 	e6 (create-entity {:kind "E" :name "e6"})]
;;     (delete-entity [e1 nil e2])
;;     (delete-entity (map :key [e3 e4 {:a :b}]))
;;     (delete-entity (merge (map map->entity [e5 e6]) {:a :b}))
;;     (are [record] (thrown? EntityNotFoundException (get-entity record))
;; 	 e1 e2 e3 e4 e5 e6)))
