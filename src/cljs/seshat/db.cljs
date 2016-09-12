(ns seshat.db
  (:require [seshat.lib.notes :as notes]))

(def default-db
  #:data{:notes []
         :display #:display{:notes []
                            :tags []
                            :selected-tags #{}}})

(defn initial-data [] default-db)

(defn update-display
  "Given a full db object, reflect any of these changes:
    * the full raw note data set
    * the set of selected tags"
  [db]
  (let [raw-notes (:data/notes db)
        ;; TODO ^ sort by update time once that's a thing
        tagged-notes (notes/filter-all raw-notes (-> db :data/display :display/selected-tags))
        tags-list (sort (notes/unique-tags tagged-notes))]
    (-> db
        (assoc-in [:data/display :display/notes] tagged-notes)
        (assoc-in [:data/display :display/tags] tags-list))))

(defn toggle [set item]
  (if (contains? set item)
    (disj set item)
    (conj set item)))

(defn click-tag [db tag]
  (-> db
      (update-in [:data/display :display/selected-tags] toggle tag)
      (update-display)))

(defn add-note [db note]
  (-> db
      (update :data/notes conj note)
      (update-display)))
