(ns seshat.test.notes
  (:require [seshat.database.protocols :as p]
            [seshat.database.impl.fake :as db]
            [seshat.auth.impl.fake :as auth]))

(defn all-notes
  ([] (all-notes (:id (first @auth/users))))
  ([user-id]
   (-> db/fake-user-data
       (p/user-filter user-id)
       (p/query nil))))

(def clear! #(reset! db/fake-database []))
