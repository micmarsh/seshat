(ns seshat.views
  (:require [re-frame.core :as re-frame]
            [seshat.lib.notes :as notes]
            [seshat.views.util :as util]))

(defn new-note-box []
  (fn []
    [:div#new-note-entry
     [:span "New Note: "]
     [:textarea
      {:on-key-up #(when (and (util/enter? %)
                              (not (empty? (util/input-text %))))
                     (util/submit-text %))}]]))

(defn editing-note-box []
  (let [currently-editing (re-frame/subscribe [:currently-editing])]
    (fn []
      (when-not (nil? @currently-editing)
        [:div#editing-note
         [:span "Edit Note: "]
         [:textarea
          {:default-value (:text @currently-editing)
           :on-key-up #(when (util/enter? %)
                         (re-frame/dispatch [:edited-note (util/input-text %) @currently-editing])
                         (re-frame/dispatch [:cancel-editing-note]))}]
         " "
         [:button {:on-click #(re-frame/dispatch [:cancel-editing-note])} "cancel"]]))))

(defn search-box []
  (fn []
    [:div#search-box
     [:span "Search: "]
     [:input {:on-change #(re-frame/dispatch [:search (util/input-text %)])}]]))

(defn notes-list []
  (let [notes (re-frame/subscribe [:notes-list])]
    (fn []
      [:div#notes-list
       [:h2 "Notes"]
       [new-note-box]
       [editing-note-box]
       [search-box]
       (doall
        (for [note @notes]
          [:div.note-content {:key (notes/id note)}
           (util/display-note (:text note))
           " "
           [:button {:on-click #(re-frame/dispatch [:start-editing-note note])}
            "Edit"]]))])))

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
