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

(defrecord user-notes [connection user-id]
  p/NewNote
  (new-note! [{:keys [connection]} text]
    (let [tx-result @(d/transact connection [{:note/text text
                                              :note/id :note.id/counter
                                              :db/id #db/id[:db.part/user]}])
          time (result-time tx-result)
          saved-note (result-note tx-result)]
      {:id (:db/id saved-note)
       :text (:note/text saved-note)
       :created time
       :updated time}))
  p/ReadNote
  (read-note [{:keys [connection]} id]
    (let [db (d/db connection)]
      (d/entity db id))))

(comment
  
  (def schema
    [{:db/id #db/id[:db.part/db]
      :db/ident :note/text
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :db/fulltext true
      :db/doc "A note's text"
      :db.install/_attribute :db.part/db}
     
     {:db/id :note.id/counter :value 0}
     {:db/id (d/tempid :db.part/user)
      :db/ident :note.id/increment
      :db/fn (d/function
              {:lang "clojure"
               :params '[db]
               :code '(let [v (:value (d/entity db :note.id/counter))]
                        (println "inc" v)
                        [{:db/id :note.id/counter
                          :value (inc v)}])})}
     
     {:db/id #db/id[:db.part/db]
      :db/ident :note/id
      :db/valueType :db.type/bigint
      :db/cardinality :db.cardinality/one
      :db/doc "A note's publically identifiable id"
      :db.install/_attribute :db.part/db}])

  (def id-schema
    [     
     {:db/id :note.id/counter :value 0}
     {:db/id (d/tempid :db.part/user)
      :db/ident :note.id/increment
      :db/fn (d/function
              {:lang "clojure"
               :params '[db]
               :code '(let [v (:value (d/entity db :note.id/counter))]
                        (println "inc" v)
                        [{:db/id :note.id/counter
                          :value (inc v)}])})}
     
     {:db/id #db/id[:db.part/db]
      :db/ident :note/id
      :db/valueType :db.type/bigint
      :db/cardinality :db.cardinality/one
      :db/doc "A note's publically identifiable id"
      :db.install/_attribute :db.part/db}])
  ;; TODO USER SHIT! Interesting business, and promising for
  ;; extensibility
  ;;  * can "not worry" about all that shit while currently just
  ;; sticking to "user notes"
  ;;  * then, when time comes can add :note/user db.type/ref. HAWT.
  ;;  * then user schema and notes from there on out all new notes get
  ;; their :note/user shit filled out. HAWT!
  
  )
