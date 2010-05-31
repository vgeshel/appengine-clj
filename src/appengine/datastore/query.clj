(ns appengine.datastore.query
  (:import (com.google.appengine.api.datastore Key Query Query$FilterOperator Query$SortDirection)))

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
   (= operator not) Query$FilterOperator/NOT_EQUAL
   :else (throw (IllegalArgumentException. (str "Invalid filter operator: " operator)))))

(defn sort-direction
  "Returns the SortDirection enum for the given sort direction. The
  direction argument must be :asc for an ascending, or :desc for
  descending a sort order.

Examples:

  (sort-direction :asc)
  ; => #<SortDirection ASCENDING>

  (sort-direction :desc)
  ; => #<SortDirection DESCENDING>
"  
  [direction]
  (cond
   (= direction :asc) Query$SortDirection/ASCENDING
   (= direction :desc) Query$SortDirection/DESCENDING
   :else (throw (IllegalArgumentException. (str "Invalid sort direction: " direction)))))

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
