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

(defn new-note [text]
  {:note/text text
   :note/id (java.util.UUID/randomUUID)
   :db/id #db/id[:db.part/user]})

(defrecord user-notes [connection user-id]
  p/NewNote
  (new-note! [{:keys [connection]} text]
    (let [tx-result @(d/transact connection [(new-note text)])
          time (result-time tx-result)
          saved-note (result-note tx-result)]
      {:id (:note/id saved-note)
       :text (:note/text saved-note)
       :created time
       :updated time}))
  p/ReadNote
  (read-note [{:keys [connection]} id]
    (let [db (d/db connection)
          entity-id (ffirst (d/q [:find '?e :where ['?e :note/id id]] db))
          note (d/entity db entity-id)
          [times] (d/q '[:find ?e (max ?tx-time) (min ?tx-time)
                       :in $ ?e
                       :where
                       [?e _ _ ?tx _]
                       [?tx :db/txInstant ?tx-time]]
                     (d/history db) entity-id)]
      {:id (:note/id note)
       :text (:note/text note)
       :created (last times)
       :updated (second times)})))

(comment
  
  (def schema
    [{:db/id #db/id[:db.part/db]
      :db/ident :note/text
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :db/fulltext true
      :db/doc "A note's text"
      :db.install/_attribute :db.part/db}
     
     {:db/id #db/id[:db.part/db]
      :db/ident :note/id
      :db/valueType :db.type/uuid
      :db/cardinality :db.cardinality/one
      :db/doc "A note's unique identifier"
      :db.install/_attribute :db.part/db}])

  (def uri "datomic:mem://notes-n-stuff")

  (d/create-database uri) (def connection (d/connect uri))
  
  (def notes-store (->user-notes connection nil))
  
  ;; TODO USER SHIT! Interesting business, and promising for
  ;; extensibility
  ;;  * can "not worry" about all that shit while currently just
  ;; sticking to "user notes"
  ;;  * then, when time comes can add :note/user db.type/ref. HAWT.
  ;;  * then user schema and notes from there on out all new notes get
  ;; their :note/user shit filled out. HAWT!
  
  )
