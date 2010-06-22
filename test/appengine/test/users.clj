(ns appengine.test.users
  (:import (com.google.appengine.api.users User UserService))
  (:use appengine.test appengine.users clojure.test))

(refer-private 'appengine.users)

(deftest test-user->map
  (let [user (User. "roman@burningswell.com" "burningswell.com" "user-id" "federated-identity")
        map (user->map user)]
    (is (= (:auth-domain map) (.getAuthDomain user)))
    (is (= (:email map) (.getEmail user)))
    (is (= (:federated-identity map) (.getFederatedIdentity user)))
    (is (= (:user-id map) (.getUserId user)))))

(deftest test-user-service
  (is (isa? (class (user-service)) UserService)))

(user-test test-current-user
  (is (nil? (current-user)))
  (is (isa? (class (user-service)) UserService)))

(user-test test-login-url
  (is (= (login-url "http://example.com")
         "/_ah/login?continue=http%3A%2F%2Fexample.com"))
  (is (= (login-url "http://example.com" "example.com")
         "/_ah/login?continue=http%3A%2F%2Fexample.com")))

(user-test test-logout-url
  (is (= (logout-url "http://example.com")
         "/_ah/logout?continue=http%3A%2F%2Fexample.com"))
  (is (= (logout-url "http://example.com" "example.com")
         "/_ah/logout?continue=http%3A%2F%2Fexample.com")))

(deftest test-user-info-given-request
  (let [user-info-map {:user "instance of User" :user-service "instance of UserService"}
        ring-request {:appengine/user-info user-info-map}]
    (is (= (user-info ring-request) user-info-map))))

(deftest test-wrap-requiring-login
  (testing "redirects to login when user isn't logged in"
    (let [fake-user-service (proxy [com.google.appengine.api.users.UserService] []
                              (isUserLoggedIn [] false)
                              (createLoginURL [dest] (str "/login?then=" dest)))
          request {:uri "/requested_path"
                   :appengine/user-info {:user nil :user-service fake-user-service}}]
      (let [wrapped-app-with-url (wrap-requiring-login #(throw (Exception.)) "/the_path")]
        (is (= {:status 302 :headers {"Location" "/login?then=/the_path"}}
               (wrapped-app-with-url request))))
      (let [wrapped-app-with-no-url (wrap-requiring-login #(throw (Exception.)))]
        (is (= {:status 302 :headers {"Location" "/login?then=/requested_path"}}
               (wrapped-app-with-no-url request))))))
  (testing "allows request to pass when user is logged in"
    (let [fake-user-service (proxy [com.google.appengine.api.users.UserService] []
                              (isUserLoggedIn [] true))
          request {:appengine/user-info {:user "instance of User" :user-service fake-user-service}}
          dummy-response {:status 200 :body "Hello"}
          dummy-app (fn [request] dummy-response)]
      (let [wrapped-app-with-url (wrap-requiring-login dummy-app "/the_path")]
        (is (= dummy-response (wrapped-app-with-url request))))
      (let [wrapped-app-with-no-url (wrap-requiring-login dummy-app)]
        (is (= dummy-response (wrapped-app-with-no-url request)))))))

(deftest test-wrap-requiring-admin
  (testing "rejects request when user is logged in but not admin"
           (let [fake-user-service (proxy [com.google.appengine.api.users.UserService] []
                                     (isUserLoggedIn [] true)
                                     (isUserAdmin [] false))
                 request {:appengine/user-info {:user "instance of User"
                                                :user-service fake-user-service}}
                 wrapped-app (wrap-requiring-admin #(throw (Exception.)))]
             (is (= {:status 403 :body "Access denied. You must be logged in as admin user!"}
                    (wrapped-app request)))))
  (testing "allows request to pass when user is logged in as admin"
           (let [fake-user-service (proxy [com.google.appengine.api.users.UserService] []
                                     (isUserLoggedIn [] true)
                                     (isUserAdmin [] true))
                 request {:appengine/user-info {:user "instance of User" :user-service fake-user-service}}
                 dummy-response {:status 200 :body "Hello"}
                 dummy-app (fn [request] dummy-response)
                 wrapped-app (wrap-requiring-admin dummy-app)]
             (is (= dummy-response (wrapped-app request))))))
