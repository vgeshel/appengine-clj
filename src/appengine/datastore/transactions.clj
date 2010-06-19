(ns #^{:author "Jean-Denis Greze"
       :doc "Datastore transaction API." }
    appengine.datastore.transactions
  (:import (com.google.appengine.api.datastore DatastoreFailureException)
	   (java.util ConcurrentModificationException))
  (:use [clojure.contrib.def :only (defvar)])
  (:require [appengine.datastore.service :as service]))

;; (defvar *transaction* nil
;;   "The current datastore transaction. If set to nil, all operations
;;   are done without a transaction (the default behaviour).")

;; (defvar *transaction-retries* 4
;;   "Total number of of times to retry a transaction beyond the first
;;   attempt in case a DatastoreFailureException is thrown.")

;; (defn new-transaction 
;;   "Returns a new GAE transaction object"
;;   [] (.beginTransaction (datastore)))

;; ;; (defn current-transaction 
;; ;;   "Returns the current transaction."
;; ;;   [] *transaction*)

;; (defn rollback-transaction 
;;   "Can be used optionally to rollback a transaction.  
;;   Use only within with-transaction.

;;   (let [[k1 k2] (tr/with-transaction
;; 		 (let [forget1 (create 
;; 				{:kind \"Person\" :name \"ForgetMe1\"})
;; 		       forget2 (create 
;; 				{:kind \"Person\" :name \"ForgetMe2\"
;; 				 :parent-key (:key forget1)})]
;; 		   (tr/rollback-transaction)
;; 		   [(:key forget1) (:key forget2)]))]
;;     (is (thrown? EntityNotFoundException (get-entity k1)))
;;     (is (thrown? EntityNotFoundException (get-entity k2)))))

;;   Note that the correct transaction is substituted in by the 
;;   with-transaction macro."
;;   ([] (rollback-transaction *transaction*))
;;   ([transaction] (.rollback transaction)))

;; (defn is-transaction-active?
;;   "Returns whether the current transaction is active. As with
;;   rollback-transaction, should only be used from within with-transaction.
;;   See doc of rollback-transaction for an example"
;;   ([] (is-transaction-active? *transaction*))
;;   ([transaction] (.isActive transaction)))

;; (defmacro with-retries
;;   "Set the number of retries for any transactions within do retries's body.
;;   The first argument is the number of retries desired.  Must be used
;;   outside of the with-transaction macro.

;;   ;; try to create parent and child a maximum of three times
;;   (tr/with-retries 2
;;     (tr/with-transaction
;;       (let [parent (create {:kind \"Person\" :name \"Jennifer\"})
;;             child (create {:kind \"Person\" :name \"Robert\"
;;                                      :parent-key (:key parent)})]
;;        [parent child])))"  
;;   [num & body]
;;   `(binding [*transaction-retries* ~num] (do ~@body)))

;; (defmacro without-transaction
;;   "Used to do no-transaction operations from within a transaction.
;;    Should be used within a try to catch any DatastoreFailureException
;;    or ConcurrentModificationException or other exceptions so that 
;;    these do not interfere with the encompassing transaction.

;;    (with-transaction
;;      (let [parent (create {:kind \"Person\" :name \"Jennifer\"})
;;            stranger (try 
;;                       (without-transaction
;;                         (create {:kind \"Person\" :name \"Chris\"}))
;;                       (catch Exception e nil))
;;            child (create {:kind \"Person\" :name \"Robert\"
;;                                     :parent-key (:key parent)})]
;;      ... do something with parent, stranger, child ...
;;     ))"
;;   [& body]
;;   `(binding [*transaction* nil] (do ~@body)))

;; (defmacro with-transaction 
;;   "Takes a body of forms (do is implied) and at runtime executes 
;;    the body of forms with a newly created transaction
;;    (one per thread per with-transaction).  In case of a 
;;    DatastoreFailureException which is not caught by the user's code,
;;    retry the transaction *transaction-retries* times 
;;    and throw the last DatastoreFailureException in case of 
;;    ultimate failure despite the retries.  Returns the value of the
;;    result of the last form.

;;    (with-transaction
;;      (let [parent (create {:kind \"Person\" :name \"Jennifer\"})
;;            child (create {:kind \"Person\" :name \"Robert\"
;;                                     :parent-key (:key parent)})]
;;        [parent child]))"
;;   [& body]
;;   `(loop [retries-left# *transaction-retries*]
;;      (let [status-and-result# 
;; 	   (try 
;; 	    (binding [*transaction* (new-transaction)]
;; 	      (let [result# (do 
;; 			      ~@body)]
;; 		;; check there has not been a user rollback/commit
;; 		(if (.isActive *transaction*) 
;; 		  (.commit *transaction*))
;; 		[true result#])
;; 	      (catch DatastoreFailureException e# 
;; 		(if (zero? retries-left#) 
;; 		  (throw e#)
;; 		  [false nil]))
;; 	      (catch ConcurrentModificationException e# 
;; 		(if (zero? retries-left#) 
;; 		  (throw e#)
;; 		  [false nil]))))]
;;        (if (= true (first status-and-result#))
;; 	 (second status-and-result#)
;; 	 (recur (dec retries-left#))))))
