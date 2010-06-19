(ns #^{:author "Roman Scherer"}
  appengine.datastore.types
  (:import (com.google.appengine.api.datastore
            Blob Category Email Entity GeoPt IMHandle IMHandle$Scheme Link PhoneNumber
            PostalAddress Rating ShortBlob Text)
           (java.net MalformedURLException URL ))
  (:use [clojure.contrib.string :only (lower-case trim)]
        [clojure.contrib.seq :only (includes?)]))

(defmulti deserialize
  "Deserialize the instance into a clojure data structure."
  (fn [instance]
    (if (isa? (class instance) Entity)
      (.getKind instance)
      (class instance))))

(defmethod deserialize Blob [blob]
  (seq (.getBytes blob)))

(defmethod deserialize Category [category]
  (.getCategory category))

(defmethod deserialize Email [email]
  (.getEmail email))

(defmethod deserialize GeoPt [geo-point]
  {:latitude (.getLatitude geo-point) :longitude (.getLongitude geo-point)})

(defmethod deserialize IMHandle [im-handle]
   {:protocol (.getProtocol im-handle) :address (.getAddress im-handle)})

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
  (if (isa? (class instance) Entity)
    (reduce #(assoc %1 (keyword (key %2)) (deserialize (val %2)))
            {:kind (.getKind instance) :key (.getKey instance)}
            (.entrySet (.getProperties instance)))
    instance))

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

(defmethod serialize Email [_ address]
  (Email. address))

(defmethod serialize Category [_ category]
  (Category. category))

(defmethod serialize GeoPt [_ location]
  (GeoPt. (:latitude location) (:longitude location)))

(defmethod serialize IMHandle [_ {:keys [protocol address]}]
  (IMHandle.
   (try
     (URL. protocol)
     (catch MalformedURLException _
       (cond
        (= protocol "sip") IMHandle$Scheme/sip
        (= protocol "xmpp") IMHandle$Scheme/xmpp
        :else IMHandle$Scheme/unknown)))
   address))

(defmethod serialize Link [_ link]
  (Link. link))

(defmethod serialize PhoneNumber [_ phone-number]
  (PhoneNumber. (str phone-number)))

(defmethod serialize PostalAddress [_ postal-address]
  (PostalAddress. postal-address))

(defmethod serialize Rating [_ rating]
  (Rating. rating))

(defmethod serialize ShortBlob [_ bytes]
  (ShortBlob. (byte-array bytes)))

(defmethod serialize String [_ string]
  (str string))

(defmethod serialize Text [_ text]
  (Text. text))

(defmethod serialize :default [type value]
  value)
