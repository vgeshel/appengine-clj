(ns appengine.datastore.query
  (:import (com.google.appengine.api.datastore Key Query Query$FilterOperator)))

(defn filter-operator
  "Returns the FilterOperator enum for the given operator. The
  operator argument is a clojure function, such as =, >, >=, <, <= or
  not. These functions are not called at all, they just act as
  shortcut to map to the FilterOperator enums.

Examples:

  (filter-operator =)
  ; => #<FilterOperator =>

  (filter-operator >)
  ; => #<FilterOperator >>

  (filter-operator not)
  ; => #<FilterOperator !=>

"
  [operator]
  (cond
   (= operator =) Query$FilterOperator/EQUAL
   (= operator >) Query$FilterOperator/GREATER_THAN
   (= operator >=) Query$FilterOperator/GREATER_THAN_OR_EQUAL
   (= operator <) Query$FilterOperator/LESS_THAN
   (= operator <=) Query$FilterOperator/LESS_THAN_OR_EQUAL
   (= operator not) Query$FilterOperator/NOT_EQUAL))

(defmulti make-query
  "Create a new Query that finds Entity objects."
  (fn [& args] (map class args)))

(defmethod make-query [] []
  (Query.))

(defmethod make-query [Key] [key]
  (Query. key))

(defmethod make-query [String] [kind]
  (Query. kind))

(defmethod make-query [String Key] [kind key]
  (Query. kind key))
