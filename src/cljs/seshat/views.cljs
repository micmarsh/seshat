(ns seshat.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [clojure.string :as str]))

(defn input-text [event]
  (-> event .-target .-value))

(defn ctrl? [event]
  (== 17 (.-keyCode event)))

(defn enter? [event]
  (== 13 (.-keyCode event)))

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

(defn clear-input [event]
  (-> event
      (.-target)
      (.-value)
      (set! ""))
  false)

(defn submit-text [event]
  (println (input-text event))
  (re-frame/dispatch [:new-note (input-text event)])
  (clear-input event))

(defn new-note-box []
  (fn []
    [:div#new-note-entry
     [:span "New Note: "]
     [:textarea
      {:on-key-up #(when (and (enter? %) (not (empty? (input-text %))))
                     (submit-text %))}]]))


(defn notes-list []
  (let [notes (re-frame/subscribe [:notes-list])]
    (fn []
      [:div#notes-list
       [:h2 "Notes"]
       [new-note-box]
       [:div#search-box
        [:span "Search: "]
        [:input {:on-change #(re-frame/dispatch [:search (input-text %)])}]]
       (doall
        (for [note @notes]
          [:div.note-content {:key (:id note (:temp-id note))}
           ;; TODO abstract that^ shit into a nice cljc lib
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
