(ns seshat.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]
              [seshat.db.auth :as auth]))

(re-frame/reg-sub
 :display
 (fn [db] (:data/display db)))

(re-frame/reg-sub
 :notes-list
 :<- [:display]
 (fn [display]
   (->> display
        (:display/notes)
        (sort-by :updated)
        (reverse))))

(re-frame/reg-sub
 :tags-list
 :<- [:display]
 (fn [display] (:display/tags display)))

(re-frame/reg-sub
 :selected-tags
 :<- [:display]
 (fn [display] (-> display :display/filters :filters/tags)))

(re-frame/reg-sub
 :currently-editing
 :<- [:display]
 (fn [display] (:display/currently-editing display)))

(re-frame/reg-sub
 :currently-uploading
 :<- [:display]
 (fn [display]
   (:display/currently-uploading display)))

(re-frame/reg-sub
 :upload-error
 :<- [:display]
 (fn [display]
   (:display/upload-error display)))

(re-frame/reg-sub
 :logged-in?
 (comp boolean auth/get-session))

(re-frame/reg-sub
 :failed-login?
 auth/login-fail?)
