(ns seshat.lib.notes
  (:refer-clojure :exclude [contains?]))

(def tag-regex #"#[\w0-9]+")

(def tags (comp (partial re-seq tag-regex) :text))

(defn contains? [tag note]
  (clojure.core/contains? (set (tags note)) tag))

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
