(ns seshat.auth.impl.fake
  (:require [seshat.session.protocols :as sp]
            [seshat.auth.protocols :as p]))

(def sessions (atom { }))

(def users (atom []))

(def user-ids (atom 0))

(defn new-fake-session [] (str (Math/abs (hash (rand)))))

(def fake-auth
  (reify
    sp/Lookup
    (lookup-session [_ key]
      (get @sessions key))
    sp/Save
    (save-session! [_ session]
      (swap! sessions assoc (:id session) session))
    sp/NewFromUser
    (from-user [_ user]
      {:id (new-fake-session) :user user})
    sp/UserId
    (user-id [_ session] (get-in session [:user :id]))
    sp/Id (id [_ session] (:id session))
    
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

