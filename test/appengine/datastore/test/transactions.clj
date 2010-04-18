(ns appengine.datastore.test.transactions
  (:require [appengine.datastore.core :as ds]
	    [appengine.datastore.transactions :as tr]
	    [appengine.datastore.entities :as en])
  (:use clojure.test
	appengine.test-utils)
  (:import (com.google.appengine.api.datastore EntityNotFoundException
                                               Query DatastoreFailureException Query$FilterOperator)))

;; appengine.datastore/core tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(dstest create-entity-in-transaction
  (let [entity (tr/with-transaction 
		(ds/create-entity {:kind "Person" :name "liz"}))]
    (is (= "liz" (:name entity))))
  (let [child (tr/with-transaction
		(let [parent (ds/create-entity {:kind "Person" :name "jane"})]
		  (ds/create-entity {:kind "Person" :name "bob" 
				     :parent-key (:key parent)})))]
    (is (= "bob" (:name child)))
    (is (= ["jane"] 
	   (map :name (ds/find-all 
		       (doto (Query. "Person") 
			 (.addFilter "name" 
				     Query$FilterOperator/EQUAL "jane"))))))))

(dstest do-rollback-in-transaction
   (let [[k1 k2] (tr/with-transaction
		 (let [forget1 (ds/create-entity 
				{:kind "Person" :name "ForgetMe1"})
		       forget2 (ds/create-entity 
				{:kind "Person" :name "ForgetMe2"
				 :parent-key (:key forget1)})]
		   (tr/rollback-transaction)
		   [(:key forget1) (:key forget2)]))]
    (is (thrown? EntityNotFoundException (ds/get-entity k1)))
    (is (thrown? EntityNotFoundException (ds/get-entity k2)))))

(defn always-fails []
  (throw (DatastoreFailureException. "Test Error")))

(dstest multiple-tries-in-transaction
  (is (thrown? DatastoreFailureException
	       (tr/with-transaction
		(always-fails)))))

(dstest non-transaction-datastore-operations-within-transactions
  (let [[k1 k2 k3 k4] 
	(tr/with-transaction
	 (let [entity (ds/create-entity {:kind "Person" :name "Entity"})
	       same-entity-group-as-entity (ds/create-entity 
					    {:kind "Person" 
					     :name "SameEntityGroupAsEntity"
					     :parent-key (:key entity)})]
	   (is (thrown? IllegalArgumentException ;; wrong entity group
			(ds/create-entity {:kind "Person" 
					   :name "WrongEntityGroup"})))
	   (map :key 
		[entity same-entity-group-as-entity
		 (tr/without-transaction  ;; do atomic request via macro
		  (ds/create-entity {:kind "Person" 
				     :name "DifferentEntityGroup"}))
		 (ds/create-entity nil ;; do atomic request manually
				   {:kind "Person"
				    :name "CanDoItManuallyAsWell"})])))]
    (is (= "Entity" (:name (ds/get-entity k1))))
    (is (= "SameEntityGroupAsEntity" (:name (ds/get-entity k2))))
    (is (= "DifferentEntityGroup" (:name (ds/get-entity k3))))
    (is (= "CanDoItManuallyAsWell" (:name (ds/get-entity k4))))))

(dstest nested-transactions
  (let [[k1 k2 k3 k4]
	(tr/with-transaction ;; transaction for first entity group
	 (let [entity (ds/create-entity {:kind "Person" :name "Entity"})
	       same-entity-group-as-entity (ds/create-entity 
					    {:kind "Person" 
					     :name "SameEntityGroupAsEntity"
					     :parent-key (:key entity)})
	       [entity2 same-entity-group-as-entity2]
		(tr/with-transaction ;; nested for a second entity group
		 (let [entity2 (ds/create-entity {:kind "Person" 
						  :name "Entity2"})
		       same-entity-group-as-entity2 (ds/create-entity 
						     {:kind "Person" 
						      :name "SameEntityGroupAsEntity2"
						      :parent-key (:key entity2)})]
		   [entity2 same-entity-group-as-entity2]))]
	   [entity same-entity-group-as-entity
	    entity2 same-entity-group-as-entity2]))]
    (is (= "Entity" (:name (ds/get-entity k1))))
    (is (= "SameEntityGroupAsEntity" (:name (ds/get-entity k2))))
    (is (= "Entity2" (:name (ds/get-entity k3))))
    (is (= "SameEntityGroupAsEntity2" (:name (ds/get-entity k4))))))

(dstest updates-with-transactions
  (let [entity (ds/create-entity {:kind "Person" :name "Entity"})]
    (tr/with-transaction
     (is (= "Entity" (:name entity)))
     (ds/update-entity entity {:day-job "hacker" :favorite-sport "beach volley"})
     (ds/update-entity entity {:day-job "entrepreneur" :night-job "hacker++"})))
  (let [[entity] (ds/find-all
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
	      
(dstest deletes-with-transactions
  (let [entity (ds/create-entity {:kind "Person" :name "Entity"})
	entity2 (ds/create-entity {:kind "Person" :name "Entity2"})]
    (tr/with-transaction
     (ds/delete-entity entity)
     (is (thrown? IllegalArgumentException (ds/delete-entity entity2))))
    (is (thrown? EntityNotFoundException (ds/get-entity (:key entity))))
    (is (= "Entity2" (:name (ds/get-entity (:key entity2)))))
    (tr/with-transaction
     (ds/delete-entity entity2)
     (tr/rollback-transaction))
    (is (= "Entity2" (:name (ds/get-entity (:key entity2)))))
    (tr/with-transaction
     (ds/delete-entity entity2))
    (is (thrown? EntityNotFoundException (ds/get-entity (:key entity2))))))

;; We can also do transactions manually if we so wish
(dstest manual-transactions
  (let [transaction (.beginTransaction (ds/datastore)) ;; manually create tr
	entity (ds/create-entity transaction {:kind "Person" :name "Entity"})
	entity2 (ds/create-entity transaction {:kind "Person" :name "Entity2"
					       :parent-key (:key entity)})
    ;; transaction not committed so these should not be in ds yet
	[entity1?] (ds/find-all
		    (doto (Query. "Person")
		      (.addFilter "name" 
				  Query$FilterOperator/EQUAL "Entity")))
	[entity2?] (ds/find-all
		    (doto (Query. "Person")
		      (.addFilter "name" 
				  Query$FilterOperator/EQUAL "Entity2")))]
    (is (nil? entity1?))
    (is (nil? entity2?))
    ;; now commit
    (.commit transaction)
    (let [[entity1?] (ds/find-all
		      (doto (Query. "Person")
			(.addFilter "name" 
				    Query$FilterOperator/EQUAL "Entity")))
	  [entity2?] (ds/find-all
		      (doto (Query. "Person")
			(.addFilter "name" 
				   Query$FilterOperator/EQUAL "Entity2")))]
      ;; post-commit we find the entities
      (is (= "Entity" (:name entity1?)))
      (is (= "Entity2" (:name entity2?))))))

;just to show with-retries was tested manually
;(dstest doretry-transaction-manual-test
;  (tr/with-retries 2
;		(try (tr/with-transaction
;		      (always-fails)) (catch Exception e (prn "error")))
;		(try (tr/with-transaction
;		      (always-fails)) (catch Exception e (prn "error")))))

;; appengine.datastore/entities
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(en/defentity testuser ()
  (name :key true)
  (job))

;; it's the same deal as entities.clj relies on core.clj which
;; support transactions
(dstest entities-macros-and-transactions
  (tr/with-transaction
   (let [user (create-testuser {:name "liz" :job "entrepreneur"})]
     (create-testuser {:name "robert" :job "secretary"
		       :parent-key (:key user)})))
  (let [an-entrepreneur (find-testuser-by-job "entrepreneur")
	bob (find-testuser-by-name "robert")]
    (is (= "secretary" (:job bob)))
    (is (= "liz" (:name an-entrepreneur)))))


  
