(ns appengine.datastore.test.query
  (:use clojure.test appengine.datastore.core appengine.datastore.query appengine.test)
  (:import (com.google.appengine.api.datastore
            Query
            Query$FilterOperator
            Query$SortDirection)))

(deftest test-filter-operator
  (testing "Valid filter operators"
    (are [operator expected]
      (is (= (filter-operator operator) expected))
      = Query$FilterOperator/EQUAL
      > Query$FilterOperator/GREATER_THAN
      >= Query$FilterOperator/GREATER_THAN_OR_EQUAL
      < Query$FilterOperator/LESS_THAN
      <= Query$FilterOperator/LESS_THAN_OR_EQUAL
      not Query$FilterOperator/NOT_EQUAL))
  (testing "Invalid filter operators"
    (is (thrown? IllegalArgumentException (filter-operator :invalid)))))

(deftest test-sort-direction
  (testing "Valid sort directions"
    (are [direction expected]
      (is (= (sort-direction direction) expected))
      :asc Query$SortDirection/ASCENDING
      :desc Query$SortDirection/DESCENDING))
  (testing "Invalid sort directions"
    (is (thrown? IllegalArgumentException (sort-direction :invalid)))))

(datastore-test test-make-query
  (testing "Build a new kind-less Query that finds Entity objects."
    (let [query (make-query)]
      (is (isa? (class query) Query))
      (is (nil? (.getKind query)))
      (is (= (str query) "SELECT *"))))
  (testing "Build a new Query that finds Entity objects with the
  specified Key as an ancestor."
    (let [query (make-query (create-key "continent" "eu"))]
      (is (isa? (class query) Query))
      (is (nil? (.getKind query)))
      (is (= (str query) "SELECT * WHERE __ancestor__ is continent(\"eu\")"))))
  (testing "Build a new Query that finds Entity objects with the
  specified kind."
    (let [query (make-query "continent")]
      (is (isa? (class query) Query))
      (is (= (.getKind query) "continent"))
      (is (= (str query) "SELECT * FROM continent"))))
  (testing "Build a new Query that finds Entity objects with the
  specified kind and with Key as an ancestor."
    (let [query (make-query "countries" (create-key "continent" "eu"))]
      (is (isa? (class query) Query))
      (is (= (.getKind query) "countries"))
      (is (= (str query) "SELECT * FROM countries WHERE __ancestor__ is continent(\"eu\")")))))
