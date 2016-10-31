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

(defn note-id
  [db note-id]
  (ffirst (d/q [:find '?e :where ['?e :note/id note-id]] db)))

(def ^:const timestamp-q
  '[:find ?e (min ?tx-time) (max ?tx-time)
    :in $ ?e
    :where
    [?e _ _ ?tx _]
    [?tx :db/txInstant ?tx-time]])

(defn entity-times [db entity-id]
  (let [[times] (d/q timestamp-q (d/history db) entity-id)]
    {:created (second times)
     :updated (last times)}))

(defn note-times [db id]
  (when-let [note-entity (note-id db id)]
    (entity-times db note-entity)))

(defn read-note-raw
  [db id]
  (when-let [note-entity (note-id db id)]
    (let [note (d/entity db note-entity)]
      {:id (:note/id note)
       :text (:note/text note)})))

(defn temp-id [data]
  (if (and (map? data) (not (contains? data :db/id)))
    (assoc data :db/id (d/tempid :db.part/user))
    data))

(defn transact
  [conn data]
  (d/transact conn (mapv temp-id data)))

(defrecord user-notes [connection user-id user-db full-db]
  p/NewNote
  (new-note! [this text]
    (let [tx-result @(transact connection [#:note{:id (java.util.UUID/randomUUID)
                                                  :text text
                                                  :deleted? false
                                                  :user-id user-id}])
          time (result-time tx-result)
          saved-note (result-note tx-result)]
      {:id (:note/id saved-note)
       :text (:note/text saved-note)
       :created time
       :updated time}))
  p/ReadNote
  (read-note [this id]
    (when-let [note (read-note-raw user-db id)]
      (merge note (note-times full-db id))))
  p/EditNote
  (edit-note! [this id text]
    (when-let [note (read-note-raw user-db id)]
      (let [tx-result @(transact connection [#:note{:id id :text text}])]
        (merge note
               (note-times (:db-after tx-result) id)
               {:text text
                :updated (result-time tx-result)}))))
  p/DeleteNote
  (delete-note! [this id]
    (if-let [note (read-note-raw user-db id)]
      (do @(transact connection [#:note{:deleted? true :id id}])
          {:deleted 1})
      {:deleted 0}))
  ;; TODO p/ImportNote
  p/QueryNotes
  (query [this _]
    (mapv (fn [[entity]]
            (let [id (:note/id entity)]
              (merge (read-note-raw user-db id)
                     (note-times full-db id))))
          (d/q '[:find (pull ?e [:note/id]) :where [?e :note/id]] user-db))))

(defn user-db
  [db user-id]
  (d/filter db
            (fn [db datom]
              (->> (:e datom)
                   (d/entity db)
                   (:note/user-id)
                   (= user-id)))))

(defn undeleted-db
  [db]
  (d/filter db
            (fn [db datom]
              (->> (:e datom)
                   (d/entity db)
                   (:note/deleted)
                   (not)))))

(defrecord user-data [connection]
  p/UserFilter
  (user-filter [_ user-id]
    (let [full-db (d/db connection)]
      (map->user-notes
       {:connection connection
        :user-id user-id
        :full-db full-db
        :user-db (-> full-db (user-db user-id) (undeleted-db))}))))

(def ^:const note-schema
  [{:db/ident :note/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/doc "A note's unique identifier"}
   {:db/ident :note/user-id
    :db/valueType :db.type/uuid
    :db/doc "A note's owner"}
   {:db/ident :note/text
    :db/valueType :db.type/string
    :db/fulltext true
    :db/doc "A note's text"}
   {:db/ident :note/deleted?
    :db/valueType :db.type/boolean
    :db/doc "Whether a note has been \"deleted\" by a user"}])

(defn initialize [uri]
  (when (d/create-database uri)
    @(d/transact (d/connect uri)
                 (mapv #(assoc % :db/id (d/tempid :db.part/db)
                               :db.install/_attribute :db.part/db
                               :db/cardinality :db.cardinality/one)
                       note-schema)))
  (->user-data (d/connect uri)))
