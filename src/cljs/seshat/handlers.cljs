(ns seshat.handlers
    (:require [re-frame.core :as re-frame]
              [seshat.db :as db]
              [seshat.config :as config]
              [ajax.edn :refer [edn-response-format]]

              [seshat.handlers.http]
              [seshat.db.auth :as auth]))

(defn reg-event-re-dispatch
  ([event-key handler] (reg-event-re-dispatch event-key [] handler))
  ([event-key middleware handler]
   (re-frame/reg-event-fx
    event-key
    middleware
    (fn [& args]
      (let [events (apply handler args)]
       {:dispatch-n (seq events)})))))

(re-frame/reg-event-fx
 :initialize
 (fn  [_ _]
   {:db (db/initial-data)
    :dispatch [:initial-login-check]}))

(reg-event-re-dispatch
 :initial-login-check
 (re-frame/inject-cofx :session)
 (fn  [{:keys [session]} _]
   (if (some? session)
     {:pull-initial-data nil} ;; TODO in future, something more cache-aware
     {:end-bad-session nil})))

(def ^:const full-query-request
  {:method :get
   :uri "/query"
   :response-format (edn-response-format)
   :on-success [:query-result]
   :on-auth-failure [:end-bad-session]
   :on-failure [:FIXME-generic-fail]})

(re-frame/reg-event-fx
 :pull-initial-data
 (constantly {:dispatch [:http full-query-request]}))

(reg-event-re-dispatch
 :query-result
 (fn [_ [_ notes]]
   (map (partial vector :add-local-note) notes)))

(re-frame/reg-event-db
 :add-local-note
 (fn [db [_ note]]
   (db/add-note db note)))

(re-frame/reg-event-db
 :click-tag
 (fn [db [_ tag]]
   (db/click-tag db tag)))

(re-frame/reg-event-db
 :search
 (fn [db [_ text]]
   (db/search-text db text)))

(reg-event-re-dispatch
 :new-note
 (fn [_ [_ text]]
   {:sync-new-note {:text text}}))

(defn temporary-id []
  (gensym (gensym (gensym))))

(reg-event-re-dispatch
 :sync-new-note
 (fn [_ [_ note]]
   (let [temp-id (temporary-id)
         note (assoc note :temp-id temp-id)]
     {:add-local-note note
      :remote-new-note note})))

(reg-event-re-dispatch
 :remote-new-note
 (fn [_ [_ note]]
   {:http {:method :post
           :uri "/command/new_note"
           :headers {"content-type" "application/edn"}
           :body (pr-str note)
           :response-format (edn-response-format)
           :on-success [:update-local-note]
           :on-auth-failure [:end-bad-session]
           :on-failure [:FIXME-generic-fail]}}))

(re-frame/reg-event-db
 :update-local-note
 (fn [db [_ note]]
   (db/edit-note db note)))

(re-frame/reg-event-db
 :start-editing-note
 (fn [db [_ note]]
   (assoc-in db [:data/display :display/currently-editing] note)))

(re-frame/reg-event-db
 :cancel-editing-note
 (fn [db [_ note]]
   (assoc-in db [:data/display :display/currently-editing] nil)))

(reg-event-re-dispatch
 :edited-note
 (fn [_ [_ new-text n]]
   (let [note (assoc n :text new-text)]
     {:update-local-note note
      :remote-edit-note note})))

(re-frame/reg-event-fx
 :remote-edit-note
 (fn [_ [_ note]]
   {:dispatch [:http {:method :put
                      :uri (str "/command/edit_note/" (:id note))
                      :headers {"content-type" "application/edn"}
                      :body (pr-str note)
                      :response-format (edn-response-format)
                      :on-success [:update-local-note]
                      :on-auth-failure [:end-bad-session]
                      :on-failure [:FIXME-generic-fail]}]}))

(reg-event-re-dispatch
 :delete-note
 (fn [_ [_ note]]
   {:delete-local-note note
    :remote-delete-note note}))

(re-frame/reg-event-db
 :delete-local-note
 (fn [db [_ note]]
   (db/delete-note db note)))

(re-frame/reg-event-fx
 :remote-delete-note
 (fn [_ [_ note]]
   {:dispatch [:http {:method :delete
                      :uri (str "/command/delete_note/" (:id note))
                      :body {}
                      :headers {"content-type" "application/edn"}
                      :response-format (edn-response-format)
                      :on-success [:FIXME-generic-success]
                      :on-auth-failure [:end-bad-session]
                      :on-failure [:FIXME-generic-fail]}]}))

(re-frame/reg-event-fx
 :upload-file
 (fn [fx [_ file]]
   {:dispatch [:http {:method :post
                      :uri "/import/fetchnotes"
                      :body file
                      :response-format (edn-response-format)
                      :on-success [:upload-success]
                      :on-auth-failure [:end-bad-session]
                      :on-failure [:upload-failure]}]
    :db (assoc-in (:db fx) [:data/display :display/currently-uploading] true)}))

(re-frame/reg-event-fx
 :upload-success
 (fn [fx [_ result]]
   {:dispatch [:query-result result]
    :db (assoc-in (:db fx) [:data/display :display/currently-uploading] false)}))

(re-frame/reg-event-fx
 :upload-failure
 (fn [fx _]
   {:db (-> (:db fx)
            (assoc-in [:data/display :display/currently-uploading] false)
            (assoc-in [:data/display :display/upload-error] true))
    :dispatch-later [{:ms 5000 :dispatch [:clear-upload-error]}]}))

(re-frame/reg-event-db
 :clear-upload-error
 (fn [db _]
   (assoc-in db [:data/display :display/upload-error] false)))

(re-frame/reg-event-fx
 "FAKE-LOGIN"
 (fn [_ [_ email password]]
   {:dispatch 
    (cond (= ["foo@bar.com" "bar"] [email password]) [:new-login "fakest-session"]
          (= "foo@bad.com" email) [:new-login "bad-session-yo"]
          :else [:FIXME-generic-fail])}))
;; re_frame.core.dispatch(cljs.core.vector("FAKE-LOGIN", "fake", "login"))                                 

(re-frame/reg-event-fx
 :new-login
 (fn [{:keys [db]} [_ session-id]]
   {:db (auth/set-session db session-id)
    :dispatch [:pull-initial-data]}))

(re-frame/reg-event-db
 :end-bad-session
 (fn [_ _] (db/initial-data)))

(re-frame/reg-event-fx
 :FIXME-generic-fail
 (fn [fail _]
   (println "fail" (:event fail))
   {}))

(re-frame/reg-event-fx
 :FIXME-generic-success
 (fn [succ _]
   (println "success" (:event succ))
   {}))

(when config/debug?
  (re-frame/reg-event-db
   :print-data
   (fn [db _]
     (println (:data/notes db))
     db))
  #_(js/setInterval #(re-frame/dispatch [:print-data]) 10000))

