(ns seshat.views
    (:require [re-frame.core :as re-frame]))

(defn input-text [event]
  (-> event .-target .-value))

(defn notes-list []
  (let [notes (re-frame/subscribe [:notes-list])]
    (fn []
      [:div#notes-list "Notes"
       [:br]
       [:input {:on-change #(re-frame/dispatch [:search (input-text %)])}]
       (doall
        (for [note @notes]
          [:div {:key (:id note)}
           (:text note)]))])))

(defn tags-list []
  (let [tags (re-frame/subscribe [:tags-list])
        selected (re-frame/subscribe [:selected-tags])]
    (fn []
      [:div#tags-list "Tags"
       [:div "Selected: "
        (if (empty? @selected)
          "(none)"
          (clojure.string/join ", " @selected))]
       (doall
        (for [tag @tags]
          [:div {:key tag
                 :on-click #(re-frame/dispatch [:click-tag tag])} tag]))])))

(defn main-panel []
  [:div#main-panel
   [notes-list]
   [tags-list]])
