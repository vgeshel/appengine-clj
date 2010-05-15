(ns appengine.datastore.test.transactions
  (:use clojure.test appengine.test appengine.datastore)
  (:import (com.google.appengine.api.datastore
            EntityNotFoundException
            Query
            DatastoreFailureException
            Query$FilterOperator)))

(defentity continent ()
  (iso-3166-alpha-2 :key true)
  (name))

(defentity country (continent)
  (iso-3166-alpha-2 :key true)
  (iso-3166-alpha-3)
  (name))

(datastore-test create-entity-in-transaction
  (let [entity (with-transaction 
		(create-entity {:kind "Person" :name "liz"}))]
    (is (= "liz" (:name entity))))
  (let [child (with-transaction
		(let [parent (create-entity {:kind "Person" :name "jane"})]
		  (create-entity {:kind "Person" :name "bob" 
				     :parent-key (:key parent)})))]
    (is (= "bob" (:name child)))
    (is (= ["jane"] 
	   (map :name (find-all 
		       (doto (Query. "Person") 
			 (.addFilter "name" 
				     Query$FilterOperator/EQUAL "jane"))))))))

(datastore-test do-rollback-in-transaction
   (let [[k1 k2] (with-transaction
		 (let [forget1 (create-entity 
				{:kind "Person" :name "ForgetMe1"})
		       forget2 (create-entity 
				{:kind "Person" :name "ForgetMe2"
				 :parent-key (:key forget1)})]
		   (rollback-transaction)
		   [(:key forget1) (:key forget2)]))]
    (is (thrown? EntityNotFoundException (get-entity k1)))
    (is (thrown? EntityNotFoundException (get-entity k2)))))

(defn always-fails []
  (throw (DatastoreFailureException. "Test Error")))

(datastore-test multiple-tries-in-transaction
  (is (thrown? DatastoreFailureException
	       (with-transaction
		(always-fails)))))

(datastore-test non-transaction-datastore-operations-within-transactions
  (let [[k1 k2 k3] 
	(with-transaction
	 (let [entity (create-entity {:kind "Person" :name "Entity"})
	       same-entity-group-as-entity (create-entity 
					    {:kind "Person" 
					     :name "SameEntityGroupAsEntity"
					     :parent-key (:key entity)})]
	   (is (thrown? IllegalArgumentException ;; wrong entity group
			(create-entity {:kind "Person" 
					   :name "WrongEntityGroup"})))
	   (map :key 
		[entity same-entity-group-as-entity
		 (without-transaction  ;; do atomic request via macro
		  (create-entity {:kind "Person" 
				     :name "DifferentEntityGroup"}))])))]
    (is (= "Entity" (:name (get-entity k1))))
    (is (= "SameEntityGroupAsEntity" (:name (get-entity k2))))
    (is (= "DifferentEntityGroup" (:name (get-entity k3))))))

(datastore-test nested-transactions
  (let [[k1 k2 k3 k4]
	(with-transaction ;; transaction for first entity group
	 (let [entity (create-entity {:kind "Person" :name "Entity"})
	       same-entity-group-as-entity (create-entity 
					    {:kind "Person" 
					     :name "SameEntityGroupAsEntity"
					     :parent-key (:key entity)})
	       [entity2 same-entity-group-as-entity2]
		(with-transaction ;; nested for a second entity group
		 (let [entity2 (create-entity {:kind "Person" 
						  :name "Entity2"})
		       same-entity-group-as-entity2 (create-entity 
						     {:kind "Person" 
						      :name "SameEntityGroupAsEntity2"
						      :parent-key (:key entity2)})]
		   [entity2 same-entity-group-as-entity2]))]
	   [entity same-entity-group-as-entity
	    entity2 same-entity-group-as-entity2]))]
    (is (= "Entity" (:name (get-entity k1))))
    (is (= "SameEntityGroupAsEntity" (:name (get-entity k2))))
    (is (= "Entity2" (:name (get-entity k3))))
    (is (= "SameEntityGroupAsEntity2" (:name (get-entity k4))))))

(datastore-test updates-with-transactions
  (let [entity (create-entity {:kind "Person" :name "Entity"})]
    (with-transaction
     (is (= "Entity" (:name entity)))
     (update-entity entity {:day-job "hacker" :favorite-sport "beach volley"})
     (update-entity entity {:day-job "entrepreneur" :night-job "hacker++"})))
  (let [[entity] (find-all
		  (doto (Query. "Person")
		    (.addFilter "day-job" 
				Query$FilterOperator/EQUAL "entrepreneur")))]
    (is (= "hacker++" (:night-job entity)))
    (is (= "Entity" (:name entity)))
    (is (nil? (:favorite-sport entity))))) 
    ;; remember how ds's put works: only commit the latest Entity put
    ;; in case the same entity is put multiple times

;; transactions and find-all works, but is restricted as per 
;; http://code.google.com/appengine/docs/java/datastore/queriesandindexes.html
;; text-search for Ancestor Queries
	      
(datastore-test deletes-with-transactions
  (let [entity (create-entity {:kind "Person" :name "Entity"})
	entity2 (create-entity {:kind "Person" :name "Entity2"})]
    (with-transaction
     (delete-entity entity)
     (is (thrown? IllegalArgumentException (delete-entity entity2))))
    (is (thrown? EntityNotFoundException (get-entity (:key entity))))
    (is (= "Entity2" (:name (get-entity (:key entity2)))))
    (with-transaction
     (delete-entity entity2)
     (rollback-transaction))
    (is (= "Entity2" (:name (get-entity (:key entity2)))))
    (with-transaction
     (delete-entity entity2))
    (is (thrown? EntityNotFoundException (get-entity (:key entity2))))))

(datastore-test test-create-entity-with-child-within-transaction
  (with-transaction
    (let [continent (create-continent {:iso-3166-alpha-2 "eu"})]
      (create-country continent {:iso-3166-alpha-2 "de"})))
  (is (= (dissoc (find-continent-by-iso-3166-alpha-2 "eu") :key)
         {:iso-3166-alpha-2 "eu", :kind "continent"}))
  (is (= (dissoc (find-country-by-iso-3166-alpha-2 "de") :key)
         {:iso-3166-alpha-2 "de", :kind "country"})))

