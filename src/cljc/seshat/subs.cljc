(ns seshat.subs
    (:require [re-frame.core :as re-frame]
              [seshat.db.auth :as auth]))

(re-frame/reg-sub
 :display
 (fn [db & _] (:data/display db)))

(re-frame/reg-sub
 :notes-list
 :<- [:display]
 (fn [display & _]
   (->> display
        (:display/notes)
        (sort-by :updated)
        (reverse))))

(re-frame/reg-sub
 :tags-list
 :<- [:display]
 (fn [display & _] (:display/tags display)))

(re-frame/reg-sub
 :selected-tags
 :<- [:display]
 (fn [display & _] (-> display :display/filters :filters/tags)))

(re-frame/reg-sub
 :currently-editing
 :<- [:display]
 (fn [display & _] (:display/currently-editing display)))

(re-frame/reg-sub
 :currently-uploading
 :<- [:display]
 (fn [display & _]
   (:display/currently-uploading display)))

(re-frame/reg-sub
 :upload-error
 :<- [:display]
 (fn [display & _]
   (:display/upload-error display)))

(re-frame/reg-sub
 :logged-in?
 (fn [db & _] (boolean (auth/get-session db))))

(re-frame/reg-sub
 :failed-login?
 (fn [db & _] (auth/login-fail? db)))
