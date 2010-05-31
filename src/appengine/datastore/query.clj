(ns appengine.datastore.query
  (:import (com.google.appengine.api.datastore Key Query Query$FilterOperator Query$SortDirection))
  (:use appengine.utils [clojure.contrib.string :only (as-str)]))

(str :test)

(defn filter-operator
  "Returns the FilterOperator enum for the given operator. The
operator argument is a clojure function, such as =, >, >=, <, <= or
not. These functions are not called at all, they just act as shortcut
to map to the FilterOperator enums.

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
direction argument must be :asc for an ascending, or :desc for a
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

(defn add-sort
  "Specify how the query results should be sorted. The first call to
add-sort will register the property that will serve as the primary
sort key. A second call to add-sort will set a secondary sort key,
etc. If no direction is given, the query results will be sorted in
ascending order of the given property.

Examples:

  (add-sort (make-query \"continents\") :iso-3166-alpha-2)
  ; => #<Query SELECT * FROM continents ORDER BY iso-3166-alpha-2>

  (-> (make-query \"continents\") (add-sort :iso-3166-alpha-2) (add-sort :name :desc))
  ; => #<Query SELECT * FROM continents ORDER BY iso-3166-alpha-2, name DESC>
"
  [query property-name & [direction]]
  (.addSort query (stringify property-name)
            (if direction (sort-direction direction) Query$SortDirection/ASCENDING)))
