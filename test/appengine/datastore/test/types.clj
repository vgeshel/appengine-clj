(ns appengine.datastore.test.types
  (import (com.google.appengine.api.datastore
           Blob Category Email GeoPt IMHandle IMHandle$Scheme Link PhoneNumber
           PostalAddress Rating ShortBlob Text))
  (:use appengine.datastore.types appengine.test clojure.test))

(refer-private 'appengine.datastore.types)

(deftest test-serialize-protocol
  (are [protocol expected]
    (is (= (serialize-protocol protocol) expected))
    "sip" IMHandle$Scheme/sip
    "xmpp" IMHandle$Scheme/xmpp
    "unknown" IMHandle$Scheme/unknown
    "http://aim.com" (java.net.URL. "http://aim.com")))

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

(deftest test-serialize-imhandle
  (let [property (serialize IMHandle {:protocol "xmpp" :address "address"})]
    (is (isa? (class property) IMHandle))
    (is (= (.getProtocol property) "xmpp"))
    (is (= (.getAddress property) "address")))
  (let [property (serialize IMHandle {:protocol "http://aim.com" :address "address"})]
    (is (isa? (class property) IMHandle))
    (is (= (.getProtocol property) "http://aim.com"))
    (is (= (.getAddress property) "address"))))

(deftest test-serialize-link
  (let [property (serialize Link "http://example.com")]
    (is (isa? (class property) Link))
    (is (= (.getValue property) "http://example.com"))))

(deftest test-serialize-phone-number
  (let [property (serialize PhoneNumber "12345")]
    (is (isa? (class property) PhoneNumber))
    (is (= (.getNumber property) "12345"))))

(deftest test-serialize-postal-address
  (let [property (serialize PostalAddress "Senefelderstraße, 10437 Berlin, Germany")]
    (is (isa? (class property) PostalAddress))
    (is (= (.getAddress property) "Senefelderstraße, 10437 Berlin, Germany"))))

(deftest test-serialize-rating
  (let [property (serialize Rating 10)]
    (is (isa? (class property) Rating))
    (is (= (.getRating property) 10))))

(deftest test-serialize-short-blob
  (let [property (serialize ShortBlob [(byte 1) (byte 2)])]
    (is (isa? (class property) ShortBlob))
    (is (= (seq (.getBytes property)) [(byte 1) (byte 2)]))))

(deftest test-serialize-text
  (let [property (serialize Text "Lorem ipsum dolor sit amet ...")]
    (is (isa? (class property) Text))
    (is (= (.getValue property) "Lorem ipsum dolor sit amet ..."))))

(deftest test-deserialize-blob
  (is (= (deserialize (Blob. (byte-array [(byte 1) (byte 2)])))
         [(byte 1) (byte 2)])))

(deftest test-deserialize-category
  (is (= (deserialize (Category. "clojure")) "clojure")))

(deftest test-deserialize-email
  (let [property (deserialize (Email. "info@example.com"))]
    (is (= property "info@example.com"))))

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
  (is (= (deserialize (PostalAddress. "Senefelderstraße, 10437 Berlin, Germany"))
         "Senefelderstraße, 10437 Berlin, Germany")))

(deftest test-deserialize-rating
  (is (= (deserialize (Rating. 10)) 10)))

(deftest test-deserialize-short-blob
  (is (= (deserialize (ShortBlob. (byte-array [(byte 1) (byte 2)])))
         [(byte 1) (byte 2)])))

(deftest test-deserialize-text
  (is (= (deserialize (Text. "Lorem ipsum dolor sit amet ..."))
         "Lorem ipsum dolor sit amet ...")))
