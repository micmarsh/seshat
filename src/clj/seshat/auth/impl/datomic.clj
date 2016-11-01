(ns seshat.auth.impl.datomic
  (:require [seshat.session.protocols :as sp]
            [seshat.auth.protocols :as p]
            [seshat.datomic.mem :refer [connection]]
            [datomic.api :as d]))

(def ^:const year-millis (* 1000 60 60 24 365))

(defn expiration-date []
  (let [date (java.util.Date.)]
    (.setTime date (+ (.getTime date) year-millis))
    date))

(defn expired? [date] (.after (java.util.Date.) date))

(defn active-sessions
  [db]
  (d/filter db
            (fn [db datom]
              (->> (:e datom)
                   (d/entity db)
                   (:session/expires)
                   (expired?)
                   (not)))))

(defrecord auth [connection]
  sp/Lookup
  (lookup-session [_ key]
    (let [id (java.util.UUID/fromString key)
          db (active-sessions (d/db connection))]
      (ffirst (d/q '[:find (pull ?e [*])
                     :in $ ?id
                     :where [?e :session/id ?id]]
                   db id))))
  sp/NewFromUser
  (from-user [_ user]
    (let [entity-id (or (:db/id user)
                        (ffirst (d/q [:find '?e :where ['?e :user/id (:user/id user)]]
                                     (d/db connection))))] ;; TODO remove `or` once user
      ;; creation is in place
      {:session/id (java.util.UUID/randomUUID)
       :session/user entity-id  
       :session/expires (expiration-date)}))
  sp/Save
  (save-session! [_ session]
    @(d/transact connection [(assoc session :db/id #db/id[:db.part/user])]))
  sp/UserId
  (user-id [_ session]
    (let [user (d/entity (d/db connection) (:db/id (:session/user session)))]
      (:user/id user)))
  sp/Id (id [_ session] (str (:session/id session)))

  p/Register
  (register! [_ name pw]
    (when-not (ffirst (d/q [:find '?e
                            :where
                            ['?e :user/name name]
                            '[?e :user/deleted? false]]
                           (d/db connection)))
      (let [new-user #:user{:id (java.util.UUID/randomUUID)
                            :name name
                            :password pw}]
        @(d/transact connection [(assoc new-user :user/deleted? false
                                        :db/id #db/id[:db.part/user])])
        new-user)))
  p/Login
  (login [_ name password]
    (if-let [name-match (ffirst (d/q [:find '(pull ?e [*])
                                       :where
                                       ['?e :user/name name]
                                       '[?e :user/deleted? false]]
                                      (d/db connection)))]
      (if (= password (:user/password name-match))
        name-match
        :bad-password)
      :no-user)))
