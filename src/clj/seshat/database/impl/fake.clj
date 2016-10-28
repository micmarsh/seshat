(ns seshat.database.impl.fake
  (:require [seshat.database.protocols :as p]
            [seshat.spec.notes :as s]))

(def now #(java.util.Date.))

(def fake-database (atom []))

(defn fake-id-gen [] (java.util.UUID/randomUUID))

(defn fake-user-database [user-id]
  (reify
    p/NewNote
    (new-note! [_ text]
      (let [note {:text text
                  :id (fake-id-gen)
                  :user-id user-id
                  :created (now)
                  :updated (now)}]
        (swap! fake-database conj note)
        note))
    p/ReadNote
    (read-note [_ id]
      (->> @fake-database
           (filter (comp #{user-id} :user-id))
           (filter (comp #{id} :id))
           (first)
           (s/trim)))
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
            (s/trim updated)))))
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
                            :id (fake-id-gen)
                            :user-id user-id)]
            (swap! fake-database conj note)
            (s/trim note)))))
    p/QueryNotes
    (query [db _]
      (->> @fake-database
           (filter (comp #{user-id} :user-id))
           (map s/trim)))))

(def fake-user-data
  (reify p/UserFilter (user-filter [_ user-id] (fake-user-database user-id))))
