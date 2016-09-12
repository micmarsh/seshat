(ns seshat.db
  (:require [seshat.lib.notes :as notes]))

(def default-db
  #:data{:notes []
         :display #:display{:notes []
                            :tags []}})

(defn initial-data [] default-db)

(defn update-display [db]
  (let [raw-notes (:data/notes db)
        ;; TODO ^ sort by update time once that's a thing
        tags-list (sort (notes/unique-tags raw-notes))]
    (-> db
        (assoc-in [:data/display :display/notes] raw-notes)
        (assoc-in [:data/display :display/tags] tags-list))))

(defn add-note [db note]
  (-> db
      (update :data/notes conj note)
      (update-display)))
