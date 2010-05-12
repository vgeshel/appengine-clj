(ns appengine.test.taskqueue
  (:import (com.google.appengine.api.labs.taskqueue Queue TaskHandle))
  (:use appengine.taskqueue appengine.test clojure.test))

(deftest test-default-queue
  (is (isa? (class (default-queue)) Queue)))

;; (task-queue-test test-add
;;   (is (isa? (class   (add "/")) TaskHandle)))

;; (with-local-task-queue
;;   (add "/"))
