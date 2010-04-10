(ns appengine.test.utils
  (:use appengine.utils clojure.test))

(deftest test-compact
  (is (= (compact []) []))
  (is (= (compact [1 nil 2]) [1 2]))
  (is (= (compact [1 2]) [1 2])))
