(ns appengine.datastore.test.types
  (import (com.google.appengine.api.datastore
           Blob Category Email Entity KeyFactory GeoPt IMHandle IMHandle$Scheme Link
           PhoneNumber PostalAddress Rating ShortBlob Text))
  (:use appengine.datastore.keys appengine.datastore.types appengine.test clojure.test))

(refer-private 'appengine.datastore.types)

(deftest test-deserialize-blob
  (is (= (deserialize (Blob. (byte-array [(byte 1) (byte 2)])))
         [(byte 1) (byte 2)])))

(deftest test-deserialize-category
  (is (= (deserialize (Category. "clojure")) "clojure")))

(deftest test-deserialize-email
  (let [property (deserialize (Email. "info@example.com"))]
    (is (= property "info@example.com"))))

(datastore-test test-deserialize-entity
  (let [entity (deserialize (Entity. (make-key "continent" "eu")))]
    (is (= (:key entity) (make-key "continent" "eu")))
    (is (= (:kind entity) "continent")))
  (let [entity (deserialize
                (doto (Entity. (make-key "continent" "eu"))
                  (.setProperty "location" (GeoPt. 54.52 15.25))))]
    (is (= (:key entity) (make-key "continent" "eu")))
    (is (= (:kind entity) "continent"))
    (is (= (:location entity) {:latitude (float 54.52) :longitude (float 15.25)}))))

(deftest test-deserialize-geopt
  (let [property (deserialize (GeoPt. 1 2))]
    (is (= property {:latitude 1 :longitude 2}))))

(deftest test-deserialize-imhandle  
  (is (= (deserialize (IMHandle. IMHandle$Scheme/xmpp "address"))
         {:protocol "xmpp" :address "address"}))
  (is (= (deserialize (IMHandle. (java.net.URL. "http://aim.com") "address"))
         {:protocol "http://aim.com" :address "address"})))

(deftest test-deserialize-link
  (is (= (deserialize (Link. "http://example.com")) "http://example.com")))

(deftest test-deserialize-phone-number
  (is (= (deserialize (PhoneNumber. "1234")) "1234")))

(deftest test-deserialize-postal-address
  (is (= (deserialize (PostalAddress. "postal address"))
         "postal address")))

(deftest test-deserialize-rating
  (is (= (deserialize (Rating. 10)) 10)))

(deftest test-deserialize-short-blob
  (is (= (deserialize (ShortBlob. (byte-array [(byte 1) (byte 2)])))
         [(byte 1) (byte 2)])))

(deftest test-deserialize-text
  (is (= (deserialize (Text. "Lorem ipsum dolor sit amet ..."))
         "Lorem ipsum dolor sit amet ...")))

(deftest test-serialize-blob
  (let [property (serialize Blob [(byte 1) (byte 2)])]
    (is (isa? (class property) Blob))
    (is (= (seq (.getBytes property)) [(byte 1) (byte 2)]))))

(deftest test-serialize-category
  (let [property (serialize Category "clojure")]
    (is (isa? (class property) Category))
    (is (= (.getCategory property) "clojure"))))

(deftest test-serialize-email
  (let [property (serialize Email "info@example.com")]
    (is (isa? (class property) Email))
    (is (= (.getEmail property) "info@example.com"))))

(deftest test-serialize-geopt
  (let [property (serialize GeoPt {:latitude 1 :longitude 2})]
    (is (isa? (class property) GeoPt))
    (is (= (.getLatitude property) 1))
    (is (= (.getLongitude property) 2))))

(deftest test-serialize-im-handle
  (are [handle protocol address]    
    (let [property (serialize IMHandle handle)]
      (is (isa? (class property) IMHandle))
      (is (= (.getProtocol property) protocol))
      (is (= (.getAddress property) address)))
    {:protocol nil :address "address"} "unknown" "address"
    {:protocol "unknown" :address "address"} "unknown" "address"
    {:protocol "sip" :address "address"} "sip" "address"
    {:protocol "xmpp" :address "address"} "xmpp" "address"
    {:protocol "http://aim.com" :address "address"} "http://aim.com" "address"))

(deftest test-serialize-link
  (let [property (serialize Link "http://example.com")]
    (is (isa? (class property) Link))
    (is (= (.getValue property) "http://example.com"))))

(deftest test-serialize-phone-number
  (let [property (serialize PhoneNumber "12345")]
    (is (isa? (class property) PhoneNumber))
    (is (= (.getNumber property) "12345"))))

(deftest test-serialize-postal-address
  (let [property (serialize PostalAddress "postal address")]
    (is (isa? (class property) PostalAddress))
    (is (= (.getAddress property) "postal address"))))

(deftest test-serialize-rating
  (let [property (serialize Rating 10)]
    (is (isa? (class property) Rating))
    (is (= (.getRating property) 10))))

(deftest test-serialize-short-blob
  (let [property (serialize ShortBlob [(byte 1) (byte 2)])]
    (is (isa? (class property) ShortBlob))
    (is (= (seq (.getBytes property)) [(byte 1) (byte 2)]))))

(deftest test-serialize-text
  (let [property (serialize Text "Lorem ipsum dolor sit amet.")]
    (is (isa? (class property) Text))
    (is (= (.getValue property) "Lorem ipsum dolor sit amet."))))
