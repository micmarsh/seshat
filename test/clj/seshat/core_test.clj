(ns seshat.core-test
  (:require [clojure.test :refer [deftest is run-tests testing use-fixtures]]
            [re-frame.core :as re-frame]

            [seshat.persist :as persist]
            [seshat.test
             [auth :as auth]
             [notes :as notes]
             [re-frame :refer [sync-dispatch-fixture]]]
            [seshat.handlers]
            [seshat.subs]
            [seshat.test.http]))

(defn clear! []
  (persist/clear!)
  (notes/clear!)
  (auth/clear!)
  (re-frame/dispatch-sync [:initialize]))

(def ^:const +email+ "foo@bar.com")
(def ^:const +password+ "bar")

(use-fixtures :once sync-dispatch-fixture)

(def ^:const sample-note-text
  ["#todo testing client logic"
   "#todo test server logic"
   "#read a book"
   "#todo #read Hans Herman-Hoppe"
   "re-frame is the best #resources #clojure"])

(defn initial-state-fixture
  [test]
  (clear!)
  (re-frame/dispatch [:user-register +email+ +password+])
  (doseq [text sample-note-text]
    (re-frame/dispatch [:new-note text]))
  (test))

(use-fixtures :each initial-state-fixture)

(deftest test-new-user-authentication
  (testing "Basic Registration"
    ;; Actual registration in default fixture
    (let [logged-in? (re-frame/subscribe [:logged-in?])
          session (persist/fetch-local "session-id")]
      (is @logged-in? "subscriptions reflects auth state")
      (is (some? session))
      (is (= session (key (first @auth/sessions)))
          "local session matches up to server record")
      (is (= 1 (count @auth/users))
          "user created")
      (is (= #:user{:name +email+ :password +password+} ;; TODO account
             ;; for hashing in near future
             (select-keys (first @auth/users) [:user/name :user/password]))
          "user has correct info")
      (testing "user can log in"
        (re-frame/dispatch [:user-login +email+ +password+])
        (let [new-session (persist/fetch-local "session-id")]
          (is @logged-in? "subscriptions still reflects auth state")
          (is (not= session new-session)
              "login overwrites old local session")
          (is (= #{session new-session} (set (keys @auth/sessions)))
              "both sessions now exist on server"))))))

(deftest test-app-note-actions
  ;; Notes added in default fixture
  (let [saved-notes (notes/all-notes)]
    (testing "initial notes properly saved in database"
      (is (= 5 (count saved-notes)))
      (is (every? #{(:user/id (first @auth/users))}
                  (map :user-id saved-notes))))
    
    (testing "UI display filters"
      (let [selected-notes (re-frame/subscribe [:notes-list])
            associated-tags (re-frame/subscribe [:tags-list])
            selected-tags (re-frame/subscribe [:selected-tags])]
        
        (testing "select one tag"
          (re-frame/dispatch [:click-tag "#todo"])
          (is (= 3 (count @selected-notes)))
          (is (= #{"#todo"} @selected-tags))
          (is (= #{"#todo" "#read"} (set @associated-tags))))
        
        (testing "select another tag"
          (re-frame/dispatch [:click-tag "#read"])
          (is (= 1 (count @selected-notes)))
          (is (= #{"#todo" "#read"} @selected-tags (set @associated-tags))))

        (testing "deselect first tag"
          (re-frame/dispatch [:click-tag "#todo"])
          (is (= 2 (count @selected-notes)))
          (is (= #{"#read"} @selected-tags))
          (is (= #{"#todo" "#read"} (set @associated-tags))))

        (testing "narrow down by (case-insensitive) search"
          (re-frame/dispatch [:search "hoppe"])
          (is (= 1 (count @selected-notes)))
          (is (= "#todo #read Hans Herman-Hoppe"
                 (:text (first @selected-notes)))))))))

(deftest test-app-note-edit-and-delete
  (let [display-notes (re-frame/subscribe [:notes-list])]
    (testing "simple delete"
      (re-frame/dispatch [:delete-note (first @display-notes)])
      (is (= 4 (count (notes/all-notes)) (count @display-notes))))
    (testing "simple edit"
      (let [edited-note (first @display-notes)]
        (re-frame/dispatch [:edited-note "#todo test editing" edited-note])
        (is (= "#todo test editing"
               (:text (notes/note (:id edited-note)))
               (:text (notes/note (:id edited-note) @display-notes))))))))

(deftest test-app-actions-while-filtered
  (let [selected-notes (re-frame/subscribe [:notes-list])
        associated-tags (re-frame/subscribe [:tags-list])
        selected-tags (re-frame/subscribe [:selected-tags])
        new-note-text "#todo #read #philosophy Matthew B. Crawford"]
    
    (testing "adding note to filtered list"
      (re-frame/dispatch [:click-tag "#todo"])
      (re-frame/dispatch [:click-tag "#read"]) 
      (testing "filtering still works normally"
        (is (= 1 (count @selected-notes)))
        (is (= #{"#todo" "#read"} @selected-tags (set @associated-tags))))
      (re-frame/dispatch [:new-note new-note-text])
      (testing "new note gets prepended onto filtered list"
        (is (= new-note-text (:text (first @selected-notes))) "ordering is as expected")
        (is (= 2 (count @selected-notes)))
        (is (= #{"#todo" "#read"} @selected-tags))
        (is (= #{"#todo" "#read" "#philosophy"}  (set @associated-tags)))))

    (testing "editing note on filitered list"
      (let [crawford-note (first @selected-notes)]
        (re-frame/dispatch [:edited-note "#authors #philosophy Matthew B. Crawford" crawford-note])
        (testing "editing tags out of note removes from filtered lists"
          (is (= 1 (count @selected-notes)))
          (is (= #{"#todo" "#read"} @selected-tags (set @associated-tags)))
          (is (= "#todo #read Hans Herman-Hoppe" (:text (first @selected-notes)))))))

    (testing "deleting note from filtered lists"
      (re-frame/dispatch [:delete-note (first @selected-notes)])
      (is (empty? @selected-notes))
      (is (empty? @associated-tags))
      (is (= #{"#todo" "#read"} @selected-tags)))))
