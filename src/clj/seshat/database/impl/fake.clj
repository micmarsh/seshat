(ns seshat.database.impl.fake
  (:require [datomic.api :as d]
            [seshat.database.impl.datomic :refer [->user-data]]))
  
(def uri "datomic:mem://fake")

(when (d/create-database uri)
  (let [note-schema
        [{:db/id #db/id[:db.part/db]
          :db/ident :note/id
          :db/valueType :db.type/uuid
          :db/unique :db.unique/identity 
          :db/cardinality :db.cardinality/one
          :db/doc "A note's unique identifier"
          :db.install/_attribute :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :note/user-id
          :db/valueType :db.type/uuid
          :db/cardinality :db.cardinality/one
          :db/doc "A note's owner"
          :db.install/_attribute :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :note/text
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one
          :db/fulltext true
          :db/doc "A note's text"
          :db.install/_attribute :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :note/deleted?
          :db/valueType :db.type/boolean
          :db/cardinality :db.cardinality/one
          :db/doc "Whether a note has been \"deleted\" by a user"
          :db.install/_attribute :db.part/db}]]
    @(d/transact (d/connect uri) note-schema)))

(def fake-user-data (->user-data (d/connect uri)))
