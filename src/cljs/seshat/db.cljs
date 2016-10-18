(ns seshat.db
  (:require [seshat.lib.notes :as notes]
            [seshat.db.auth :as auth]))

(def default-db
  #:data{:notes []
         :auth #:auth{:session-id nil
                      :login-fail false}
         :display #:display{:notes []
                            :tags []
                            :filters #:filters{:tags #{}
                                               :search ""}
                            :currently-editing nil
                            :currently-uploading false
                            :upload-error false}})

(defn initial-data
  ([] (initial-data {}))
  ([{:keys [session] :as persisted}]
   (cond-> default-db
     session (auth/set-session session))))

(defn apply-filters [notes filters]
  (->> notes
       (notes/filter-tags (:filters/tags filters))
       (notes/filter-text (:filters/search filters))))

(defn update-display
  "Given a display object and a set of notes, use the display's current filters
   to derive a new set of notes and tags, updating the display"
  [display notes]
  (let [filtered-notes (apply-filters notes (:display/filters display))
        tags-list (sort (notes/unique-tags filtered-notes))]
    (-> display
        (assoc :display/notes filtered-notes)
        (assoc :display/tags tags-list))))

(defn toggle [set item]
  (if (contains? set item)
    (disj set item)
    (conj set item)))

(defn click-tag [db tag]
  (-> db
      (update-in [:data/display :display/filters :filters/tags] toggle tag)
      (update :data/display update-display (:data/notes db))))

(defn search-text [db text]
  (-> db
      (assoc-in [:data/display :display/filters :filters/search] text)
      (update :data/display update-display (:data/notes db))))

(defn add-note [db note]
  (let [new-notes (conj (:data/notes db) note)]
    (-> db
        (assoc :data/notes new-notes)
        (update :data/display update-display new-notes))))

(defn without-note [note existing-notes]
  (into (empty existing-notes)
        (remove (partial notes/== note))
        existing-notes))

(defn update-existing-note [existing-notes note]
  (if (first (filter (partial notes/== note) existing-notes))
    (->> existing-notes
         (without-note note)
         (cons (dissoc note :temp-id))
         (vec))
    (throw (ex-info "Note not found locally!"
                    {:note note
                     :local-data existing-notes}))))

(defn edit-note [db note]
  (let [new-notes (update-existing-note (:data/notes db) note)]
    (-> db
        (assoc :data/notes new-notes)
        (update :data/display update-display new-notes))))


(defn delete-note [db note]
  (let [new-notes (without-note note (:data/notes db))]
    (-> db
        (assoc :data/notes new-notes)
        (update :data/display update-display new-notes))))
