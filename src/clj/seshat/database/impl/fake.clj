(ns seshat.database.impl.fake
  (:require [seshat.database.protocols :as p]))

(def now #(java.util.Date.))

(def fake-database (atom []))

(def fake-id-gen (atom 0))

(defn fake-user-database [user-id]
  (reify
    p/NewNote
    (new-note! [_ text]
      (let [note {:text text
                  :id (swap! fake-id-gen inc)
                  :user-id user-id
                  :created (now)
                  :updated (now)}]
        (swap! fake-database conj note)
        note))
    p/ReadNote
    (read-note [_ id]
      (first (filter (comp #{id} :id)
                     (filter (comp #{user-id} :user-id)
                             @fake-database))))
    p/EditNote
    (edit-note! [this id text]
      (locking fake-database
        (when-let [note (p/read-note this id)]
          (let [updated (assoc note :text text :updated (now))]
            (swap! fake-database (fn [data]
                                   (->> data
                                        (remove (comp #{id} :id))
                                        (cons updated)
                                        (vec))))
            updated))))
    p/DeleteNote
    (delete-note! [_ id]
      (locking fake-database
        (let [before @fake-database]
          (swap! fake-database (fn [data]
                                 (->> data
                                      (remove (fn [n]
                                                (and (= user-id (:user-id n))
                                                     (= id (:id n)))))          
                                      (vec))))
          {:deleted (- (count before) (count @fake-database))})))
    p/ImportNote
    (import-note! [_ {id :fetchnotes/id :as data}]
      (locking fake-database
        (when (empty? (filter (comp #{id} :fetchnotes/id) @fake-database))
          (let [note (assoc data
                            :id (swap! fake-id-gen inc)
                            :user-id user-id)]
            (swap! fake-database conj note)
            note))))
    p/QueryNotes
    (query [db _] (filter (comp #{user-id} :user-id) @fake-database))))

(def fake-user-data
  (reify p/UserFilter (user-filter [_ user-id] (fake-user-database user-id))))
