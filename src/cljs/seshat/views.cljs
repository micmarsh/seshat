(ns seshat.views
  (:require [re-frame.core :as re-frame]
            [clojure.string :as str]))

(defn input-text [event]
  (-> event .-target .-value))

(defn tag-span [tag]
  [:span.tag-text
   {:on-click #(re-frame/dispatch [:click-tag tag])
    :key tag} tag])

(defn display-word [word]
  (if (str/starts-with? word "#")
    (tag-span word)
    word))

(defn display-note [note-text]
  (let [note-pieces (str/split note-text #" ")]
    (->> note-pieces
         (map display-word)
         (interpose " "))))

(defn notes-list []
  (let [notes (re-frame/subscribe [:notes-list])]
    (fn []
      [:div#notes-list
       [:h2 "Notes"]
       [:div#search-box
        [:span "Search: "]
        [:input {:on-change #(re-frame/dispatch [:search (input-text %)])}]]
       (doall
        (for [note @notes]
          [:div.note-content {:key (:id note)}
           (display-note (:text note))]))])))

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
                (mapcat (fn [tag] [" " (tag-span tag)]))
                @selected))]
       (doall
        (for [tag @tags]
          (assoc (tag-span tag) 0 :div.tag-text)))])))

(defn main-panel []
  [:div#main-panel
   [notes-list]
   [tags-list]])
