(ns seshat.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :display
 (fn [db] (:data/display db)))

(re-frame/reg-sub
 :notes-list
 :<- [:display]
 (fn [display] (:display/notes display)))

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
