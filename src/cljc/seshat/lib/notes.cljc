(ns seshat.lib.notes
  (:refer-clojure :exclude [contains? ==])
  (:require [clojure.core :as c]
            [clojure.string :as s]))

(def tag-regex #"#[\w0-9]+")

(def tags (comp (partial re-seq tag-regex) :text))

(defn contains? [tag note]
  (c/contains? (set (tags note)) tag))

(defn == [& notes]
  ;; generate fake ids w/ gensym so multiple nils don't cause false match
  (or (apply = (map #(:id % (gensym)) notes))
      (apply = (map #(:temp-id % (gensym)) notes))))

(defn id [{:keys [id temp-id] :as note}]
  (or id temp-id
      (throw (ex-info "note data has no valid id"
                      {:data note}))))

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
   #?(:cljs (comp (partial re-find (js/RegExp. text "i")) s/lower-case :text)
      :clj #(.contains ^String (s/lower-case (:text %)) text))
   notes))
