(ns seshat.test.notes
  (:require [seshat.datomic.mem :refer [connection]]
            [seshat.database.impl.datomic :as datom]
            [datomic.api :as d]))

(defn -read-note [db id]
  (when-let [entity-id (ffirst (d/q [:find '?e :where ['?e :note/id id] '[?e :note/deleted? false]] db))]
    (let [note (d/entity db entity-id)
          [times] (d/q datom/timestamp-q (d/history db) entity-id)]
      {:id (:note/id note)
       :text (:note/text note)
       :user-id (:note/user-id note)
       :created (second times)
       :updated (last times)})))

(defn all-notes []
  (let [db (d/db @connection)]
    (mapv (comp (partial -read-note db) :note/id first)
          (d/q [:find '(pull ?e [:note/id]) :where '[?e :note/deleted? false]] db))))

(defn clear! []
  (let [notes (all-notes)]
    @(d/transact @connection
                 (mapv (fn [note]
                         {:db/id (d/tempid :db.part/user)
                          :note/deleted? true
                          :note/id (:id note)})
                       notes))))

(defn note
  ([id] (note id (all-notes)))
  ([id note-list] (first (filter (comp #{id} :id) note-list))))
