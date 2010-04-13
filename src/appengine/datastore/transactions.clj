(ns appengine.datastore.transactions
  (:require [appengine.datastore.core :as ds]
	    [clojure.zip :as zip])
  (:import (com.google.appengine.api.datastore DatastoreFailureException)
	   (java.util ConcurrentModificationException)))

;; WARNING: Currently
;; Be careful when doing calls to the ds through other functions.  
;; Use *thread-local-transaction* when designing new functions 
;; to get the current transaction for the current thread 
;; (may be nil; the datastore treats passing in
;; a nil transaction as a request to create a new transaction for 
;; that request only).
;;
;; See http://code.google.com/appengine/docs/java/javadoc/com/google/appengine/api/datastore/DatastoreService.html
;; and
;; http://books.google.fr/books?id=6cL_kCZ4NJ4C&pg=PA173&lpg=PA173&dq=null+transaction+google+appengine&source=bl&ots=sIibLUPUdj&sig=3Iy0rbJh4HAjbMudnWrj_RlgGtc&hl=en&ei=sHXCS4z1DYn80wT45oGnCQ&sa=X&oi=book_result&ct=result&resnum=7&ved=0CBkQ6AEwBjgK#v=onepage&q&f=false

(def 
#^{:doc 
  "Total number of of times to retry a transaction beyond the first
   attempt in case a DatastoreFailureException is thrown."}
   *transaction-retries* 4)

(defn new-transaction 
  "Returns a new GAE transaction object"
  [] (.beginTransaction (ds/datastore)))

(defn rollback-transaction 
  "Can be used optionally to rollback a transaction.  
  Use only within with-transaction. E.g., pseudo-code:
  (with-transaction (try .... (catch ConcurrentModificationException e
                             (if (is-transaction-active?)
                                (rollback-transaction) 
                                (throw (CustomException.))))))
  Note that the correct transaction is substituted in by the 
  with-transaction macro."
  ([] (rollback-transaction ds/*thread-local-transaction*))
  ([transaction] (.rollback transaction)))

(defn is-transaction-active?
  "Returns whether the current transaction is active. As with
  rollback-transaction, should only be used from within with-transaction.
  See doc of rollback-transaction for an example"
  ([] (is-transaction-active? ds/*thread-local-transaction*))
  ([transaction] (.isActive transaction)))

(defmacro with-retries
  "Set the number of retries for any transactions within do retries's body.
  The first argument is the number of retries desired."
  [num & body]
  `(binding [*transaction-retries* ~num] (do ~@body)))

(defmacro without-transaction
  "Used to do no-transaction operations from within a transaction.
   Should be used within a try to catch any DatastoreFailureException
   or ConcurrentModificationException or other exceptions so that 
   these do not interfere with the encompassing transaction."
  [& body]
  `(binding [ds/*thread-local-transaction* nil] (do ~@body)))

(defmacro with-transaction 
  "Takes a body of forms (do is implied) and at runtime executes 
   the body of forms with a newly created transaction
   (one per thread per with-transaction).  In case of a 
   DatastoreFailureException which is not caught by the user's code,
   retry the transaction *transaction-retries* times 
   and throw the last DatastoreFailureException in case of 
   ultimate failure despite the retries.  Returns the value of the
   result of the last form."
  [& body]
  `(loop [retries-left# *transaction-retries*]
     (let [status-and-result# 
	   (try 
	    (binding [ds/*thread-local-transaction* (new-transaction)]
	      (let [result# (do 
			      ~@body)]
		;; check there has not been a user rollback/commit
		(if (.isActive ds/*thread-local-transaction*) 
		  (.commit ds/*thread-local-transaction*))
		[true result#])
	      (catch DatastoreFailureException e# 
		(if (zero? retries-left#) 
		  (throw e#)
		  [false nil]))
	      (catch ConcurrentModificationException e# 
		(if (zero? retries-left#) 
		  (throw e#)
		  [false nil]))))]
       (if (= true (first status-and-result#))
	 (second status-and-result#)
	 (recur (dec retries-left#))))))
