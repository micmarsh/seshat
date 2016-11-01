(ns seshat.handlers
  (:require [re-frame.core :as re-frame]
            [re-frame.handlers :refer [reg-event-re-dispatch]]
            [seshat.db :as db]
            [seshat.config :as config]
            [ajax.edn :refer [edn-response-format]]
            
            [seshat.handlers.http]
            [seshat.handlers.session]
            [seshat.handlers.omnibar]
            [seshat.db.auth :as auth]
            
            [clojure.spec :as s]
            [seshat.spec.client]))

(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check failed: " explain-data) explain-data)))))

(def validate-spec-mw
  (if config/debug?
    (re-frame/after (fn [db & _] (check-and-throw :client/db db)))
    []))

(re-frame/reg-event-fx
 :initialize
 [validate-spec-mw (re-frame/inject-cofx :session)]
 (fn  [{:keys [session]} _]
   {:db (db/initial-data {:session session})
    :dispatch [:initial-login-check]}))

(reg-event-re-dispatch
 :initial-login-check
 (re-frame/inject-cofx :session)
 (fn  [{:keys [session]} _]
   (if (some? session)
     {:pull-initial-data nil} ;; TODO in future, something more cache-aware
     {:end-session nil})))

(def ^:const full-query-request
  {:method :get
   :uri "/query"
   :response-format (edn-response-format)
   :on-success [:query-result]
   :on-auth-failure [:end-session]
   :on-failure [:FIXME-generic-fail]})

(reg-event-re-dispatch
 :pull-initial-data
 (constantly {:http full-query-request}))

(reg-event-re-dispatch
 :query-result
 (fn [_ [_ notes]]
   (map (partial vector :add-local-note) notes)))

(re-frame/reg-event-db
 :add-local-note
 validate-spec-mw
 (fn [db [_ note]]
   (db/add-note db note)))

(re-frame/reg-event-db
 :click-tag
 validate-spec-mw
 (fn [db [_ tag]]
   (db/click-tag db tag)))

(re-frame/reg-event-db
 :search
 validate-spec-mw
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
           :on-auth-failure [:end-session]
           :on-failure [:FIXME-generic-fail]}}))

(re-frame/reg-event-db
 :update-local-note
 validate-spec-mw
 (fn [db [_ note]]
   (db/edit-note db note)))

(re-frame/reg-event-db
 :start-editing-note
 validate-spec-mw
 (fn [db [_ note]]
   (assoc-in db [:data/display :display/currently-editing] note)))

(re-frame/reg-event-db
 :cancel-editing-note
 validate-spec-mw
 (fn [db [_ note]]
   (assoc-in db [:data/display :display/currently-editing] nil)))

(reg-event-re-dispatch
 :edited-note
 (fn [_ [_ new-text n]]
   (let [note (assoc n :text new-text)]
     {:update-local-note note
      :remote-edit-note note})))

(reg-event-re-dispatch
 :remote-edit-note
 (fn [_ [_ note]]
   {:http {:method :put
           :uri "/command/edit_note"
           :headers {"content-type" "application/edn"}
           :body (pr-str note)
           :response-format (edn-response-format)
           :on-success [:update-local-note]
           :on-auth-failure [:end-session]
           :on-failure [:FIXME-generic-fail]}}))

(reg-event-re-dispatch
 :delete-note
 (fn [_ [_ note]]
   {:delete-local-note note
    :remote-delete-note note}))

(re-frame/reg-event-db
 :delete-local-note
 validate-spec-mw
 (fn [db [_ note]]
   (db/delete-note db note)))

(reg-event-re-dispatch
 :remote-delete-note
 (fn [_ [_ note]]
   {:http {:method :delete
           :uri "/command/delete_note" 
           :body (pr-str note)
           :headers {"content-type" "application/edn"}
           :response-format (edn-response-format)
           :on-success [:FIXME-generic-success]
           :on-auth-failure [:end-session]
           :on-failure [:FIXME-generic-fail]}}))

(re-frame/reg-event-fx
 :upload-file
 validate-spec-mw
 (fn [fx [_ file]]
   {:dispatch [:http {:method :post
                      :uri "/import/fetchnotes"
                      :body file
                      :response-format (edn-response-format)
                      :on-success [:upload-success]
                      :on-auth-failure [:end-session]
                      :on-failure [:upload-failure]}]
    :db (assoc-in (:db fx) [:data/display :display/currently-uploading] true)}))

(re-frame/reg-event-fx
 :upload-success
 validate-spec-mw
 (fn [fx [_ result]]
   {:dispatch [:query-result result]
    :db (assoc-in (:db fx) [:data/display :display/currently-uploading] false)}))

(re-frame/reg-event-fx
 :upload-failure
 validate-spec-mw
 (fn [fx _]
   {:db (-> (:db fx)
            (assoc-in [:data/display :display/currently-uploading] false)
            (assoc-in [:data/display :display/upload-error] true))
    :dispatch-later [{:ms 5000 :dispatch [:clear-upload-error]}]}))

(re-frame/reg-event-db
 :clear-upload-error
 validate-spec-mw
 (fn [db _]
   (assoc-in db [:data/display :display/upload-error] false)))

(reg-event-re-dispatch
 :user-login
 (fn [_ [_ name password]]
   {:http {:method :post
           :uri "/login"
           :headers {"content-type" "application/edn"}
           :body (pr-str {:name name :password password})
           :response-format (edn-response-format)
           :on-success [:new-session]
           :on-auth-failure [:failed-login]
           :on-failure [:FIXME-generic-fail]}}))

(reg-event-re-dispatch
 :user-register
 (fn [_ [_ name password]]
   {:http {:method :post
           :uri "/register"
           :headers {"content-type" "application/edn"}
           :body (pr-str {:name name :password password})
           :response-format (edn-response-format)
           :on-success [:new-session]
           :on-failure [:FIXME-generic-fail]}}))

(re-frame/reg-event-fx
 :failed-login
 validate-spec-mw
 (fn [{:keys [db]} _]
   {:db (auth/login-fail db true)
    :dispatch-later [{:ms 5000 :dispatch [:clear-failed-login]}]}))

(re-frame/reg-event-db
 :clear-failed-login
 validate-spec-mw
 (fn [db _] (auth/login-fail db false)))

(reg-event-re-dispatch
 :new-session
 (fn [_ [_ {session-id :session}]]
   {:save-local-session session-id
    :pull-initial-data nil}
   ;; we could have ourselves a race condition right here
   ))

(reg-event-re-dispatch :logout (constantly {:end-session nil}))
;; TODO logout route for actual server sessions

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

