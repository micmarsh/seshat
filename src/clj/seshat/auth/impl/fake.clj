(ns seshat.auth.impl.fake
  (:require [seshat.session.protocols :as sp]
            [seshat.auth.protocols :as p]))

(def sessions (atom { }))

(def users (atom []))

(def user-ids (atom 0))

(def fake-auth
  (reify
    sp/Lookup
    (lookup-session [_ key]
      (get @sessions key))
    sp/Save
    (save-session! [_ key val]
      (swap! sessions assoc key val))
    p/Register
    (register! [_ email pw]
      (when-not (first (filter (comp #{email} :email) @users))
        (let [new-user {:id (swap! user-ids inc)
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

