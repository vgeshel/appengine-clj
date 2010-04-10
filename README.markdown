# Clojure library for Google App Engine

This library is based on John Hume's
[appengine-clj](http://github.com/duelinmarkers/appengine-clj), with
some enhancements. It is heavily refactored, so don't expect
compatibility with the original library. 

### appengine.datastore.core

A convenience API for the com.google.appengine.api.datastore package
providing access to Google's schema-free datastore. It allows you to
work with immutable data structures instead of mutable Entity
instances.

Example:

<pre><code>
(create-entity {:kind "continent" :name "Europe"})
=> {:kind "country" :key #<Key Person(1138)> :name "Europe"}
</code></pre>

### appengine.datastore.entities

A convenience API for the appengine.datastore namespace. Entity helper
functions can be generated with the defentity macro.

Examples:

<pre><code>

(defentity continent ()
  (iso-3166-alpha-2 :key true)
  (name))

(defentity country (continent)
  (iso-3166-alpha-2 :key true)
  (iso-3166-alpha-3)
  (name))

(defentity region (country)
  (code :key true)
  (name))

(with-local-datastore
  (let [continent (create-continent {:name "Europe" :iso-3166-alpha-2 "eu"})
        country (create-country continent {:name "Spain" :iso-3166-alpha-2 "es"})
        region (create-region country {:name "Galicia" :code "SP58"})]
    (println continent)
    (println country)
    (println region)))

; this prints: 

{:name Europe, :iso-3166-alpha-2 eu, :kind continent, :key #<Key continent("eu")>}
{:name Spain, :iso-3166-alpha-2 es, :parent-key #<Key continent("eu")>, :kind country, :key #<Key continent("eu")/country("es")>}
{:code SP58, :name Galicia, :parent-key #<Key continent("eu")/country("es")>, :kind region, :key #<Key continent("eu")/country("es")/region("SP58")>}

</code></pre>

### appengine.users

Convenience API for the com.google.appengine.api.users package.

The <code>user-info</code> function can be called from anywhere to get
the current user and a UserService.  For a more functional approach, a
Ring middlware function is provided to assoc the user info into every
request under the key :appengine-clj/user-info.



### appengine.test-utils

Provides setup and teardown of an in-memory Datastore for use in tests
or from a REPL.  If you're using <code>clojure.contrib.test-is</code>,
you can use the <code>dstest</code> macro to get a fresh Datastore for
each test.

---

Copyright (c) 2009, 2010 John D. Hume, Roman Scherer.

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

