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

(re-frame/reg-event-fx
 :pull-initial-data
 (constantly
  {:http-xhrio {:method :get
                :uri "/query"
                :response-format (edn-response-format)
                :on-success [:query-result]
                :on-failure [:initial-query-fail]}}))

(re-frame/reg-event-fx
 :initial-query-fail
 (fn [fail _]
   (println fail)
   {}))

(re-frame/reg-event-fx
 :query-result
 (fn [_ [_ notes]]
   {:dispatch-n (map (partial vector :incoming-note) notes)}))

(re-frame/reg-event-db
 :incoming-note
 (fn [db [_ note]]
   (db/add-note db note)))

(re-frame/reg-event-db
 :click-tag
 (fn [db [_ tag]]
   (db/click-tag db tag)))
