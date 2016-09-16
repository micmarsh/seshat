(ns seshat.views
  (:require [re-frame.core :as re-frame]
            [seshat.views.util :as util]))

(defn new-note-box []
  (fn []
    [:div#new-note-entry
     [:span "New Note: "]
     [:textarea
      {:on-key-up #(when (and (util/enter? %)
                              (not (empty? (util/input-text %))))
                     (util/submit-text %))}]]))


(defn notes-list []
  (let [notes (re-frame/subscribe [:notes-list])]
    (fn []
      [:div#notes-list
       [:h2 "Notes"]
       [new-note-box]
       [:div#search-box
        [:span "Search: "]
        [:input {:on-change #(re-frame/dispatch [:search (util/input-text %)])}]]
       (doall
        (for [note @notes]
          [:div.note-content {:key (:id note (:temp-id note))}
           ;; TODO abstract that^ shit into a nice cljc lib
           (util/display-note (:text note))]))])))

(defn tags-list []
  (let [tags (re-frame/subscribe [:tags-list])
        selected (re-frame/subscribe [:selected-tags])]
    (fn []
      [:div#tags-list
       [:h2 "Tags"]
       [:div#selected-tags "Selected: "
        (if (empty? @selected)
          "(none)"
          (into ()
                (mapcat (fn [tag] [" " (util/tag-span tag)]))
                @selected))]
       (doall
        (for [tag @tags]
          (assoc (util/tag-span tag) 0 :div.tag-text)))])))

(defn main-panel []
  [:div#main-panel
   [notes-list]
   [tags-list]])
