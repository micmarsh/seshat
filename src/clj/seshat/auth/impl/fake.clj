(ns seshat.auth.impl.fake
  (:require [seshat.session.protocols :as sp]
            [seshat.auth.protocols :as p]
            [datomic.api :as d]))

(def users (atom []))

(def ^:const year-millis (* 1000 60 60 24 365))

(defn expiration-date []
  (let [date (java.util.Date.)]
    (.setTime date (+ (.getTime date) year-millis))
    date))

(defn expired? [date] (.before (java.util.Date.) date))

(defn active-sessions
  [db]
  (d/filter db
              (fn [db datom]
                (->> (:e datom)
                     (d/entity db)
                     (:session/expires)
                     (expired?)))))

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
    {:session/id (java.util.UUID/randomUUID)
     :user/id (:user/id user (:id user)) ;; TODO remove or once user
     ;; creation is in place
     :session/expires (expiration-date)})
  sp/Save
  (save-session! [_ session]
    @(d/transact connection [(assoc session :db/id #db/id[:db.part/user])]))
  sp/UserId
  (user-id [_ session] (:user/id session))
  sp/Id (id [_ session] (str (:session/id session))))



(def ^:const auth-schema
  [{:db/ident :user/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/doc "A user's unique identifier"}
   {:db/ident :user/email
    :db/valueType :db.type/string
    :db/doc "A user's email"}
   {:db/ident :user/password
    :db/valueType :db.type/string
    :db/doc "A user's password"}
   {:db/ident :session/id
    :db/valueType :db.type/uuid
    :db/doc "A session's unique identifier"}
   {:db/ident :session/expires
    :db/valueType :db.type/instant
    :db/doc "The time a given session expires"}])

(defn initialize
  ;; TODO this + schema is going to need to be consolidated with notes stuff
  [uri]
  (when (d/create-database uri)
    @(d/transact (d/connect uri)
                 (mapv #(assoc % :db/id (d/tempid :db.part/db)
                               :db.install/_attribute :db.part/db
                               :db/cardinality :db.cardinality/one)
                       auth-schema)))
  (->auth (d/connect uri)))

(def fake-auth-test (initialize "datomic:mem://auth"))

(def fake-auth
  (reify
    sp/Lookup
    (lookup-session [_ key] (sp/lookup-session fake-auth-test key))
    sp/Save
    (save-session! [_ session] (sp/save-session! fake-auth-test session))
    sp/NewFromUser 
    (from-user [_ user] (sp/from-user fake-auth-test user))
    sp/UserId
    (user-id [_ session] (:user/id session))
    sp/Id (id [_ session] (str (:session/id session)))
    
    p/Register
    (register! [_ email pw]
      (when-not (first (filter (comp #{email} :email) @users))
        (let [new-user {:id (java.util.UUID/randomUUID)
                        :email email
                        :password pw}]
          (swap! users conj new-user)
          new-user)))
    p/Login
    (login [_ email password]
      (if-let [email-match (first (filter (comp #{email} :email) @users))]
        (if (= password (:password email-match))
          email-match
          :bad-password)
        :no-user))))
