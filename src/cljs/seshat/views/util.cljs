(ns seshat.views.util
  (:require [re-frame.core :as re-frame]
            [clojure.string :as str]
            [seshat.lib.notes :as notes]))

(defn input-text [event]
  (-> event .-target .-value))

(defn ctrl? [event]
  (== 17 (.-keyCode event)))

(defn enter? [event]
  (== 13 (.-keyCode event)))

(defn tag? [word]
  (let [tag (re-find notes/tag-regex word)]
    (= tag word)))

(defn tag-span
  ([tag]
   (if (tag? tag)
     [:span.tag-text
      {:on-click #(re-frame/dispatch [:click-tag tag])
       :key tag} tag]
     (let [real-tag (re-find notes/tag-regex tag)]
       (tag-span real-tag (second (str/split tag (js/RegExp. real-tag "i")))))))
  ([tag rest]
   [:span {:key tag} (tag-span tag) rest]))

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
