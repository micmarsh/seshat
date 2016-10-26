(ns seshat.test.notes
  (:require [seshat.database.impl.fake :as db]
            [seshat.auth.impl.fake :as auth]))

(defn all-notes [] @db/fake-database)

(def clear! #(reset! db/fake-database []))

(defn note
  ([id] (note id @db/fake-database))
  ([id note-list]
   (first (filter (comp #{id} :id) note-list))))
