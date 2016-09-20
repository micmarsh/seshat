(ns seshat.database.impl.fake
  (:require [seshat.database.protocols :as p]))

(def now #(java.util.Date.))

(def ^:const fake-data
  (mapv
   #(assoc % :created (now) :updated (now))
   [{:id 1
     :text "#todo use some real data"}
    {:id 2
     :text "#music #wewlad Beethoven all the symphonies"}
    {:id 3
     :text "remember #todo stuff other than this side project #today"}
    {:id 4 :text "yes pleas"}
    {:id 5 :text "#wewlad this is a cool app"}]))

(def fake-database (atom fake-data))

(def fake-id-gen (atom (:id (last fake-data))))

(def fake-impl
  (reify
    p/NewNote
    (new-note! [_ text]
      (let [note {:text text
                  :id (swap! fake-id-gen inc)
                  :created (now)
                  :updated (now)}]
        (swap! fake-database conj note)
        note))
    p/ReadNote
    (read-note [_ id]
      (first (filter (comp #{id} :id) @fake-database)))
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
                                      (remove (comp #{id} :id))          
                                      (vec))))
          {:deleted (- (count before) (count @fake-database))})))
    p/ImportNote
    (import-note! [_ {id :fetchnotes/id :as data}]
      (locking fake-database
        (when (empty? (filter (comp #{id} :fetchnotes/id) @fake-database))
          (let [note (assoc data :id (swap! fake-id-gen inc))]
            (swap! fake-database conj note)
            note))))
    p/QueryNotes
    (query [db _] @fake-database)))

