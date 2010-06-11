(ns appengine.datastore.types
  (:import (com.google.appengine.api.datastore
            Blob Category Email GeoPt IMHandle IMHandle$Scheme Link PhoneNumber
            PostalAddress Rating ShortBlob Text))
  (:use [clojure.contrib.string :only (lower-case trim)]
        [clojure.contrib.seq :only (includes?)]))

(defn- serialize-protocol [protocol]
  (try
    (java.net.URL. protocol)
    (catch java.net.MalformedURLException _
      (cond
       (= protocol "sip") IMHandle$Scheme/sip
       (= protocol "xmpp") IMHandle$Scheme/xmpp
       :else IMHandle$Scheme/unknown))))

(defmulti serialize
  "Serialize the value into the type.

Examples:

  (serialize Email \"info@example.com\")
  ; => #<Email com.google.appengine.api.datastore.Email@2d3de6b>

  (serialize GeoPt {:latitude 1 :longitude 2})
  ; => #<GeoPt 1.000000,2.000000>
"
  (fn [type value] type))

(defmethod serialize Blob [_ bytes]
  (Blob. (byte-array bytes)))

(defmethod serialize GeoPt [_ location]
  (GeoPt. (:latitude location) (:longitude location)))

(defmethod serialize IMHandle [_ handle]
  (IMHandle. (serialize-protocol (:protocol handle)) (:address handle)))

(defmethod serialize Rating [_ rating]
  (Rating. rating))

(defmethod serialize ShortBlob [_ bytes]
  (ShortBlob. (byte-array bytes)))

(defmethod serialize :default [type value]
  (eval `(new ~type ~value)))

(defmulti deserialize
  "Deserialize the instance into a clojure data structure."
  (fn [instance] (class instance)))

(defmethod deserialize Blob [blob]
  (seq (.getBytes blob)))

(defmethod deserialize Category [category]
  (.getCategory category))

(defmethod deserialize Email [email]
  (.getEmail email))

(defmethod deserialize GeoPt [geo-point]
   {:latitude (.getLatitude geo-point) :longitude (.getLongitude geo-point)})

(defmethod deserialize IMHandle [handle]
   {:protocol (.getProtocol handle) :address (.getAddress handle)})

(defmethod deserialize Link [link]
  (.getValue link))

(defmethod deserialize PhoneNumber [phone-number]
  (.getNumber phone-number))

(defmethod deserialize PostalAddress [postal-address]
  (.getAddress postal-address))

(defmethod deserialize Rating [rating]
  (.getRating rating))

(defmethod deserialize ShortBlob [short-blob]
  (seq (.getBytes short-blob)))

(defmethod deserialize Text [text]
  (.getValue text))

(defmethod deserialize :default [instance]
  instance)
