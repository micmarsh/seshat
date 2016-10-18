(ns seshat.handlers.session
  (:require [re-frame.core :as re-frame]
            [seshat.persist :as storage]
            [seshat.db.auth :as auth]
            [seshat.db :as db]))

(def ^:const session-key "session-id")

(re-frame/reg-fx
 :clear-local-session
 (fn [& _] (storage/delete-local! session-key)))

(re-frame/reg-fx
 :save-local-session
 (fn [session-id]
   (storage/persist-local! session-key session-id)))

(re-frame/reg-event-fx
 :end-bad-session
 (fn [{:keys [db]} _]
   {:db (db/initial-data)
    :clear-local-session nil}
   ;; TODO will need to clear out persisted notes/"data" once that's a thing
   ))

(re-frame/reg-event-fx
 :save-local-session
 (fn [{:keys [db]} [_ session-id]]
   {:db (auth/set-session db session-id)
    :save-local-session session-id}))

(re-frame/reg-cofx
 :session
 (fn [{:keys [db] :as coeffects}]
   (let [session (or (auth/get-session db)
                     (storage/fetch-local session-key))]
    (assoc coeffects :session session))))
