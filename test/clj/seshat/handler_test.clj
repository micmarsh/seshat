(ns seshat.handler-test
  (:require [clojure.test :refer [deftest is run-tests testing use-fixtures]]
            [seshat.test.http :refer [fake-http-request]]
            [clojure.spec :as s]

            [seshat.auth.impl.fake :refer [fake-auth]]
            [seshat.session.protocols :as sp]))

(def ^:const fake-user {:id (java.util.UUID/randomUUID)})
(def ^:dynamic *session-id* nil)

(use-fixtures :once
  (fn [test]
    (let [session (sp/from-user fake-auth fake-user)]
      (sp/save-session! fake-auth session)
      (binding [*session-id* (sp/id fake-auth session)]
        (test)))))

(defn request [method uri data]
  {:method method
   :uri uri
   :headers {"content-type" "application/edn"
             "session-id" *session-id*}
   :body (pr-str data)})

(def new-note (partial request :post "/command/new_note"))

(def edit-note (partial request :put "/command/edit_note"))

(defn predicate [error]
  (-> error :clojure.spec/problems first :pred))

(deftest test-new-edit-param-validation
  (testing "New note route"

    (let [no-text (fake-http-request (new-note {:temp-id 'temp}))]
      (is (= 400 (:status no-text)))
      (is (= '(contains? % :text) (predicate (:body no-text)))))
    
    (let [no-id (fake-http-request (new-note {:text "hello"}))]
      (is (= 400 (:status no-id)))
      (is (= '(contains? % :temp-id) (predicate (:body no-id)))))
    
    (let [full-note {:text "hello" :temp-id 'temp}
          good-response (fake-http-request (new-note full-note))]
      (is (= 201 (:status good-response)))
      (is (= full-note (select-keys (:body good-response) [:text :temp-id])))
      (is (s/valid? :note/full (:body good-response)))))

  (testing "Edit note route"
    (let [note (:body (fake-http-request (new-note {:text "hello" :temp-id 'temp})))]
      
      (let [no-text (fake-http-request (edit-note {:id (:id note)}))]
        (is (= 400 (:status no-text)))
        (is (= '(contains? % :text) (predicate (:body no-text)))))
      
      (let [good-response (fake-http-request (edit-note {:id (:id note) :text "goodbye"}))]
        (is (= 200 (:status good-response)))
        (is (= {:text "goodbye" :id (:id note)}
               (select-keys (:body good-response) [:id :text])))
        (is (s/valid? :note/full (:body good-response)))))))
