(ns seshat.datomic.schema
  (:require [datomic.api :as d]))

(def auth-schema
  [{:db/ident :user/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/doc "A user's unique identifier"}
   {:db/ident :user/name
    :db/valueType :db.type/string
    :db/unique :db.unique/identity
    :db/doc "A user's email"}
   {:db/ident :user/deleted?
    :db/valueType :db.type/boolean
    :db/doc "Whether or not a user is deleted"}
   {:db/ident :user/password
    :db/valueType :db.type/string
    :db/doc "A user's password"}
   
   {:db/ident :session/user
    :db/valueType :db.type/ref
    :db/doc "Reference to a session's user"}
   {:db/ident :session/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/doc "A session's unique identifier"}
   {:db/ident :session/expires
    :db/valueType :db.type/instant
    :db/doc "The time a given session expires"}])

(def note-schema
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

(def ^:const full
  (mapv #(assoc % :db/id (d/tempid :db.part/db)
                :db.install/_attribute :db.part/db
                :db/cardinality :db.cardinality/one)
        (concat auth-schema note-schema)))
