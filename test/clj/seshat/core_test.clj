(ns seshat.core-test
  (:require [day8.re-frame.test :as rf-test]
            [clojure.test :refer [deftest is run-tests testing]]
            [re-frame.core :as re-frame]

            [seshat.handlers]
            [seshat.test.http]))

(deftest test-new-user-registration
  )

(comment
  (defmacro with-sync-dispatches
    [& body]
    `(rf-test/run-test-sync
      (rf-test/with-temp-re-frame-state
        (re-frame/reg-fx :dispatch re-frame/dispatch)
        (re-frame/reg-fx :dispatch-n (partial run! re-frame/dispatch))
        ~@body)))
  
  (with-sync-dispatches
    (re-frame/dispatch [:user-register "foo" "bar"]))
  
  )
