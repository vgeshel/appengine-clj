(ns appengine.datastore.transactions
  (:require [appengine.datastore.core :as ds]
	    [clojure.zip :as zip])
  (:import (com.google.appengine.api.datastore DatastoreFailureException)))

;; WARNING: Currently
;; Be careful when doing calls to the ds through over functions in 
;; a transaction.  Use *thread-local-transaction* at run-time when
;; designing new functions to get the current transaction for the
;; current thread (may be nil, but the datastore treats passing in
;; a nil transaction as a request to create a new transaction for 
;; that request only.
;;
;; See http://code.google.com/appengine/docs/java/javadoc/com/google/appengine/api/datastore/DatastoreService.html
;; and
;; http://books.google.fr/books?id=6cL_kCZ4NJ4C&pg=PA173&lpg=PA173&dq=null+transaction+google+appengine&source=bl&ots=sIibLUPUdj&sig=3Iy0rbJh4HAjbMudnWrj_RlgGtc&hl=en&ei=sHXCS4z1DYn80wT45oGnCQ&sa=X&oi=book_result&ct=result&resnum=7&ved=0CBkQ6AEwBjgK#v=onepage&q&f=false

(def 
#^{:doc 
  "Total number of of times to try a transaction in case a 
   DatastoreFailureException is thrown."}
   *transaction-retries* 8)

;(def 
; #^{:doc 
;  "List of functions for which a transaction argument is added 
;  as part of the dotransaction macro. Note: there is no namespace check,
;  only a simple string-compare with the names hereunder.  Be wary of 
;  conflicts with other modules."}
; *transaction-functions*
; '("get-entity"
;   "put-entity"
;   "update-entity"
;   "delete-entity"
;   "find-all"
;   "rollback-transaction"
;   "is-transaction-active?"
;   "create-entity"))

(defn new-transaction 
  "Returns a new GAE transaction object"
  [] (.beginTransaction (ds/datastore)))

(defn rollback-transaction 
  "Can be used optionally to rollback a transaction.  
  Use only within dotransaction. E.g., pseudo-code:
  (dotransaction (try .... (catch ConcurrentModificationException e
                             (if (is-transaction-active?)
                                (rollback-transaction) 
                                (throw (CustomException.))))))
  Note that the correct transaction is substituted in by the 
  dotransaction macro."
  ([] (rollback-transaction ds/*thread-local-transaction*))
  ([transaction] (.rollback transaction)))

(defn is-transaction-active?
  "Returns whether the current transaction is active. As with
  rollback-transaction, should only be used from within dotransaction.
  See doc of rollback-transaction for an example"
  ([] (is-transaction-active? ds/*thread-local-transaction*))
  ([transaction] (.isActive transaction)))

;(defn- add-transaction? 
;  "Returns true if the function name passed in is one that supports
;  GAE transactions"
;  [function-name]
;  (let [interim (some #(.endsWith (str function-name) %) *transaction-functions*)]
;    (prn function-name " " interim)
;    interim))

;(defn- arg-adder 
;  "Takes a zipper that refers to a syntax tree, and adds
;  a transaction as the first argument of any functions that support
;  GAE transactions (i.e., all the functions in *transaction-functions*)"
;  [loc]
;  (if (zip/end? loc)
;    loc
;    (if (and 
;	 (add-transaction? (zip/node loc))
;	 (not (zip/branch? loc)) (= nil (zip/left loc)))
;      (recur (zip/next (zip/insert-right loc `*thread-local-transaction*)))
;      (recur (zip/next loc)))))

(defmacro notransaction
  "Used to do no-transaction operations from within a transaction"
  [& body]
  `(binding [ds/*thread-local-transaction* nil] (do ~@body)))

(defmacro dotransaction 
  "Takes a body of forms (do is implied), modifies the syntax-tree
   at compile-time to take a transaction argument for 
   any function that supports it (see list in *transaction-functions)),
   at runtime executes the functions with a newly created transaction
   (one per thread per dotransaction).  In case of a 
   DatastoreFailureException which is not caught by the user's code,
   retry the transaction a total of *transaction-retries* times 
   and throw the last DatastoreFailureException in case of 
   ultimate failure despite the retries.  Returns the value of the
   last form passed in in case of success."
  [& body]
;  (let [form-zipper (zip/seq-zip body)
;	new-form (zip/root (arg-adder form-zipper))] 
  `(loop [retries-left# *transaction-retries*]
     (let [status-and-result# 
	   (try 
	    (binding [ds/*thread-local-transaction* (new-transaction)]
	      (let [result# (do ~@body)]
		;; check there has not been a user rollback/commit
		(if (.isActive ds/*thread-local-transaction*) 
		  (.commit ds/*thread-local-transaction*))
		[true result#])
	      (catch DatastoreFailureException e# 
		(if (zero? retries-left#) 
		  (throw e#)
		  [false nil]))))]
       (if (= true (first status-and-result#))
	 (second status-and-result#)
	 (recur (dec retries-left#))))))
