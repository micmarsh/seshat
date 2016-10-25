(ns seshat.core-test
  (:require [day8.re-frame.test :as rf-test]
            [clojure.test :refer [deftest is run-tests testing use-fixtures]]
            [re-frame.core :as re-frame]

            [seshat.persist :as persist]
            [seshat.test.auth :as auth]
            [seshat.handlers]
            [seshat.subs]
            [seshat.test.http]))

(defmacro with-sync-dispatches
    [& body]
    `(rf-test/run-test-sync
      (rf-test/with-temp-re-frame-state
        (re-frame/reg-fx :dispatch re-frame/dispatch)
        (re-frame/reg-fx :dispatch-n (partial run! re-frame/dispatch))
        ~@body)))

(defn clear! []
  (persist/clear!)
  (auth/clear!))

(def ^:const +email+ "foo@bar.com")
(def ^:const +password+ "bar")

(use-fixtures :once
  (fn [test] (with-sync-dispatches (test)))
  (fn [test] (re-frame/dispatch [:initialize]) (test)))

(deftest test-new-user-authentication
  (testing "Basic Registration"
    (clear!)
    (re-frame/dispatch [:user-register +email+ +password+])
    (let [logged-in? (re-frame/subscribe [:logged-in?])
          session (persist/fetch-local "session-id")]
      (is @logged-in? "subscriptions reflects auth state")
      (is (some? session))
      (is (= session (key (first @auth/sessions)))
          "local session matches up to server record")
      (is (= 1 (count @auth/users))
          "user created")
      (is (= {:email +email+ :password +password+} ;; TODO account
             ;; for hasing in near future
             (select-keys (first @auth/users) [:email :password]))
          "user has correct info")
      (testing "user can log in"
        (re-frame/dispatch [:user-login +email+ +password+])
        (let [new-session (persist/fetch-local "session-id")]
          (is @logged-in? "subscriptions still reflects auth state")
          (is (not= session new-session)
              "login overwrites old local session")
          (is (= #{session new-session} (set (keys @auth/sessions)))
              "both sessions now exist on server"))))))
