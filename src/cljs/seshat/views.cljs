(ns seshat.views
    (:require [re-frame.core :as re-frame]))

(defn notes-list []
  (let [notes (re-frame/subscribe [:notes-list])]
    (fn []
      [:div#notes-list "Notes"
       (doall
        (for [note @notes]
          [:div {:key (:id note)}
           (:text note)]))])))

(defn tags-list []
  (let [tags (re-frame/subscribe [:tags-list])]
    (fn []
      [:div#tags-list "Tags"
       (doall
        (for [tag @tags]
          [:div {:key tag} tag]))])))

(defn main-panel []
  [:div#main-panel
   [notes-list]
   [tags-list]])
