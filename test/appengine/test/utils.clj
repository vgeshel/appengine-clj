(ns appengine.test.utils
  (:use appengine.utils clojure.test))

(deftest test-compact
  (is (= (compact []) []))
  (is (= (compact [1 nil 2]) [1 2]))
  (is (= (compact [1 2]) [1 2])))

(deftest test-map-keys-with-keyword
  (is (= (map-keys {"a" 1 "b" 2} #'identity) {"a" 1 "b" 2}))
  (is (= (map-keys {"a" 1 "b" 2} #'keyword) {:a 1 :b 2})))

(deftest test-map-keyword
  (is (= (map-keyword {"a" 1 "b" 2}) {:a 1 :b 2}))
  (is (= (map-keyword {'a 1 'b 2}) {:a 1 :b 2}))
  (is (= (map-keyword {:a 1 :b 2}) {:a 1 :b 2})))

