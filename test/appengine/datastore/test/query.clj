(ns appengine.datastore.test.query
  (:import (com.google.appengine.api.datastore Query Query$FilterOperator Query$SortDirection))
  (:refer-clojure :exclude [sort-by])
  (:use clojure.test appengine.datastore.core appengine.datastore.query appengine.test))

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

(datastore-test test-select
  (testing "Build a new kind-less Query that finds Entity objects."
    (let [query (select)]
      (is (query? query))
      (is (nil? (.getKind query)))
      (is (= (str query) "SELECT *"))))
  (testing "Build a new Query that finds Entity objects with the
  specified Key as an ancestor."
    (let [query (select (create-key "continent" "eu"))]
      (is (query? query))
      (is (nil? (.getKind query)))
      (is (= (str query) "SELECT * WHERE __ancestor__ is continent(\"eu\")"))))
  (testing "Build a new Query that finds Entity objects with the
  specified kind."
    (let [query (select "continent")]
      (is (query? query))
      (is (= (.getKind query) "continent"))
      (is (= (str query) "SELECT * FROM continent"))))
  (testing "Build a new Query that finds Entity objects with the
  specified kind and with Key as an ancestor."
    (let [query (select "countries" (create-key "continent" "eu"))]
      (is (query? query))
      (is (= (.getKind query) "countries"))
      (is (= (str query) "SELECT * FROM countries WHERE __ancestor__ is continent(\"eu\")"))))
  (testing "Compound select queries"
    (are [query expected-sql]
      (do (is (query? query))
          (is (= (str query) expected-sql)))
      (-> (select "continents")
          (filter-by :iso-3166-alpha-2 = "eu")
          (filter-by :country-count > 0)
          (sort-by :iso-3166-alpha-2 :desc))
      "SELECT * FROM continents WHERE iso-3166-alpha-2 = eu AND country-count > 0 ORDER BY iso-3166-alpha-2 DESC")))

(datastore-test test-filter-by
  (are [query expected-sql]
    (do (is (query? query))
        (is (= (str query) expected-sql)))
    (filter-by (select "continents") :iso-3166-alpha-2 = "eu")
    "SELECT * FROM continents WHERE iso-3166-alpha-2 = eu"
    (-> (select "continents")
        (filter-by :iso-3166-alpha-2 = "eu")
        (filter-by :name = "Europe"))
    "SELECT * FROM continents WHERE iso-3166-alpha-2 = eu AND name = Europe"))

(datastore-test test-sort-by
  (are [query expected-sql]
    (do (is (query? query))
        (is (= (str query) expected-sql)))
    (sort-by (select "continents") :iso-3166-alpha-2)
    "SELECT * FROM continents ORDER BY iso-3166-alpha-2"
    (sort-by (select "continents") :iso-3166-alpha-2 :asc)
    "SELECT * FROM continents ORDER BY iso-3166-alpha-2"
    (sort-by (select "continents") :iso-3166-alpha-2 :desc)
    "SELECT * FROM continents ORDER BY iso-3166-alpha-2 DESC"
    (-> (select "continents")
        (sort-by :iso-3166-alpha-2)
        (sort-by :name :desc))
    "SELECT * FROM continents ORDER BY iso-3166-alpha-2, name DESC"))

(datastore-test test-query
  (are [arg expected] (is (= (query? arg) expected))
       (Query.) true
       nil false
       "" false))
