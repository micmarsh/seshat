(ns seshat.handlers
    (:require [re-frame.core :as re-frame]
              [seshat.db :as db]

              [ajax.core :as ajax]
              [ajax.edn :refer [edn-response-format]]))

(re-frame/reg-event-fx
 :initialize-db
 (fn  [_ _]
   {:db (db/initial-data)
    :dispatch [:pull-initial-data]}))

(def ^:const full-query-request
  {:method :get
   :uri "/query"
   :response-format (edn-response-format)
   :on-success [:query-result]
   :on-failure [:FIXME-generic-fail]})

(re-frame/reg-event-fx
 :pull-initial-data
 (constantly {:http-xhrio full-query-request}))

(re-frame/reg-event-fx
 :query-result
 (fn [_ [_ notes]]
   {:dispatch-n (map (partial vector :add-local-note) notes)}))

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

(re-frame/reg-event-fx
 :new-note
 (fn [_ [_ text]]
   {:dispatch [:sync-new-note {:text text}]}))

(defn temporary-id []
  (gensym (gensym (gensym))))

(re-frame/reg-event-fx
 :sync-new-note
 (fn [_ [_ note]]
   (let [temp-id (temporary-id)
         note (assoc note :temp-id temp-id)]
     {:dispatch-n [[:add-local-note note]
                   [:remote-new-note note]]})))

(re-frame/reg-event-fx
 :remote-new-note
 (fn [_ [_ note]]
  {:http-xhrio {:method :post
                :uri "/command/new_note"
                :headers {"content-type" "application/edn"}
                :body (pr-str note)
                :response-format (edn-response-format)
                :on-success [:update-local-note]
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

(re-frame/reg-event-fx
 :edited-note
 (fn [_ [_ new-text n]]
   (let [note (assoc n :text new-text)]
     {:dispatch-n [[:update-local-note note]
                   [:remote-edit-note note]]})))

(re-frame/reg-event-fx
 :remote-edit-note
 (fn [_ [_ note]]
   {:http-xhrio {:method :put
                 :uri (str "/command/edit_note/" (:id note))
                 :headers {"content-type" "application/edn"}
                 :body (pr-str note)
                 :response-format (edn-response-format)
                 :on-success [:update-local-note]
                 :on-failure [:FIXME-generic-fail]}}))

(re-frame/reg-event-fx
 :delete-note
 (fn [_ [_ note]]
   {:dispatch-n [[:delete-local-note note]
                 [:remote-delete-note note]]}))

(re-frame/reg-event-db
 :delete-local-note
 (fn [db [_ note]]
   (db/delete-note db note)))

(re-frame/reg-event-fx
 :remote-delete-note
 (fn [_ [_ note]]
   {:http-xhrio {:method :delete
                 :uri (str "/command/delete_note/" (:id note))
                 :body {}
                 :headers {"content-type" "application/edn"}
                 :response-format (edn-response-format)
                 :on-success [:FIXME-generic-success]
                 :on-failure [:FIXME-generic-fail]}}))

(re-frame/reg-event-fx
 :upload-file
 (fn [_ [_ file]]
   {:http-xhrio {:method :post
                 :uri "/import/fetchnotes"
                 :body file
                 :response-format (edn-response-format)
                 :on-success [:FIXME-generic-success]
                 :on-failure [:FIXME-generic-fail]}}))

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

;; DEBUGGING
(re-frame/reg-event-db
 :print-data
 (fn [db _]
   (println (:data/notes db))
   db))

;(js/setInterval #(re-frame/dispatch [:print-data]) 10000)

