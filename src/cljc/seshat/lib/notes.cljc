(ns seshat.lib.notes
  (:refer-clojure :exclude [contains? ==])
  (:require [clojure.core :as c]))

(def tag-regex #"#[\w0-9]+")

(def tags (comp (partial re-seq tag-regex) :text))

(defn contains? [tag note]
  (c/contains? (set (tags note)) tag))

(defn == [& notes]
  (let [fake-ids (atom 0)]
    ;; ^ generate fake ids so multiple nil :ids don't cause false match
    (or (apply c/== (map #(:id % (swap! fake-ids dec)) notes))
        (apply = (map #(:temp-id % (gensym)) notes)))))

;; Collection-based
(defn unique-tags [notes]
  (sequence (comp (mapcat tags)
                  (distinct))
            notes))

(defn filter-tags [tags notes]
  (reduce (fn [notes tag]
            (filter (partial contains? tag) notes))
          notes tags))

(defn filter-text [text notes]
  (filter
   #?(:cljs (comp (partial re-find (js/RegExp. text "i")) :text)
      :clj #(.contains ^String (:text %) text))
   notes))
