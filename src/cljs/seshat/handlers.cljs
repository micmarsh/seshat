(ns seshat.handlers
    (:require [re-frame.core :as re-frame]
              [seshat.db :as db]

              [ajax.core :as ajax]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   (js/setTimeout #(re-frame/dispatch [:no-fx-lol]) 3000)
   db/default-db))

(re-frame/reg-event-fx
 :no-fx-lol
 (fn [_ _]
   {:http-xhrio {:method :get
                 :uri "/hello"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:yo]}}))

(re-frame/reg-event-db
 :yo
 (fn [db [_ result]]
   (println result)
   (assoc db :name result)))
