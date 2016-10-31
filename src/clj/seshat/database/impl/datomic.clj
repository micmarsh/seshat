(ns seshat.database.impl.datomic
  (:require [seshat.database.protocols :as p]
            [datomic.api :as d]
            [seshat.spec.notes :as s]))

(defn result-note [tx-result]
  {:pre [(= 1 (count (:tempids tx-result)))]}
  (let [note-id (second (first (:tempids tx-result)))]
    (d/entity (:db-after tx-result) note-id)))

(def tx-id (comp d/t->tx d/basis-t))

(defn result-time [tx-result]
  (let [db (:db-after tx-result)]
    (-> db
        (d/entity (tx-id db))
        (:db/txInstant))))

(defn note-tx
  ([text] (note-tx (java.util.UUID/randomUUID) text))
  ([id text]
   {:db/id (d/tempid :db.part/user)
    :note/text text
    :note/id id}))

(defn existing-notes [query user-id]
  (into query
        ['[?e :note/deleted? false]
         ['?e :note/user-id user-id]]))

(defn user-database [{:keys [db connection]}]
  (or db (d/db connection)))

(def ^:const timestamp-q
  '[:find ?e (min ?tx-time) (max ?tx-time) 
    :in $ ?e
    :where
    [?e _ _ ?tx _]
    [?tx :db/txInstant ?tx-time]])

(defn -read-note [db user-id id]
  (when-let [entity-id (ffirst (d/q (existing-notes [:find '?e :where ['?e :note/id id]] user-id) db))]
    (let [note (d/entity db entity-id)
          [times] (d/q timestamp-q (d/history db) entity-id)]
      {:id (:note/id note)
       :text (:note/text note)
       :created (second times)
       :updated (last times)})))

(defrecord user-notes [connection user-id]
  p/NewNote
  (new-note! [{:keys [connection]} text]
    (let [tx-result @(d/transact connection [(assoc (note-tx text)
                                                    :note/deleted? false
                                                    :note/user-id user-id)])
          time (result-time tx-result)
          saved-note (result-note tx-result)]
      {:id (:note/id saved-note)
       :text (:note/text saved-note)
       :created time
       :updated time}))
  p/ReadNote
  (read-note [this id]
    (-read-note (user-database this) user-id id))
  p/EditNote
  (edit-note! [{:keys [connection] :as this} id text]
    (let [db (user-database this)]
      (when-let [note (-read-note db user-id id)]
        (let [tx-result @(d/transact connection [(note-tx id text)])]
          (assoc note
                 :text text
                 :updated (result-time tx-result))))))
  p/DeleteNote
  (delete-note! [{:keys [connection] :as this} id]
    (let [db (user-database this)]
      (if-let [note (-read-note db user-id id)]
        (do @(d/transact connection [{:db/id (d/tempid :db.part/user)
                                      :note/deleted? true
                                      :note/id id}])
            {:deleted 1})
        {:deleted 0})))
  ;; TODO p/ImportNote
  p/QueryNotes
  (query [this _]
    (let [db (user-database this)]
      (mapv (comp (partial -read-note db user-id) :note/id first)
            (d/q (existing-notes [:find '(pull ?e [:note/id]) :where] user-id) db)))))

(defrecord user-data [connection]
  p/UserFilter
  (user-filter [_ user-id]
    (->user-notes connection user-id)))
