(ns appengine.memcache
  (:import java.util.Date
           [com.google.appengine.api.memcache
            Expiration
            MemcacheServiceFactory
            MemcacheService
            MemcacheService$SetPolicy]))

(defn memcache []
  (MemcacheServiceFactory/getMemcacheService))

(defn clear-all []
  (.clearAll (memcache)))

(defn get-stats []
  (let [stats (.getStatistics (memcache))]
    (if stats
      {:hits (.getHitCount stats)
       :misses (.getMissCount stats)
       :byte-hits (.getBytesReturnedForHits stats)
       :items (.getItemCount stats)
       :bytes (.getTotalItemBytes stats)
       :oldest-item-age (/ (.getMaxTimeWithoutAccess stats) 1000.0)})))

(defn get-value [#^Key key]
  (.get (memcache) key))

(defn get-native-policy [policy-keyword]
  (condp = policy-keyword
      :add MemcacheService$SetPolicy/ADD_ONLY_IF_NOT_PRESENT
      :replace MemcacheService$SetPolicy/REPLACE_ONLY_IF_PRESENT
      :set MemcacheService$SetPolicy/SET_ALWAYS
      MemcacheService$SetPolicy/SET_ALWAYS))

(defn get-expiration [amount]
  (cond
   (or (nil? amount) (= 0 amount)) nil
   (= (class amount) 'Expiration) amount
   (= (class amount) 'Date) (Expiration/onDate amount)
   (> amount (* 86400 30)) (Expiration/onDate (Date. (* amount 1000)))
   :else (Expiration/byDeltaMillis (* amount 1000))))

(defn put-value
  ([#^Key key value expiration policy] (.put (memcache) key value (get-expiration expiration) (get-native-policy policy)))
  ([#^Key key value expiration] (put-value key value expiration :set))
  ([#^Key key value] (put-value key value nil :set)))

(defn set-value
  ([#^Key key value expiration] (put-value key value expiration :set))
  ([#^Key key value] (set-value key value nil :set)))

(defn add-value
  ([#^Key key value expiration] (put-value key value expiration :add))
  ([#^Key key value] (add-value key value nil :add)))

(defn replace-value
  ([#^Key key value expiration] (put-value key value expiration :replace))
  ([#^Key key value] (replace-value key value nil :replace)))

(defn delete-value [#^Key key ms]
  (.delete (memcache) key ms))

(defn inc-value [#^Key key & delta]
  (.increment (memcache) key (or delta 1)))

(defn dec-value [#^Key key & delta]
  (.increment (memcache) key (- (or delta 1))))
