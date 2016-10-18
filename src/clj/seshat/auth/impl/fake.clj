(ns seshat.auth.impl.fake
  (:require [seshat.session.protocols :as sp]
            [seshat.auth.protocols :as p]))

(def sessions (atom { }))

(def fake-auth
  (reify
    sp/Lookup
    (lookup-session [_ key]
      (get @sessions key))
    sp/Save
    (save-session! [_ key val]
      (swap! sessions assoc key val))
    p/Login
    (login [_ email password]
      (cond (= ["foo@bar.com" "bar"] [email password]) {:id 1 :email email}
            (= ["foob@bar.com" "bar"] [email password]) {:id 2 :email email}
            (#{"foob@bar.com" "foo@bar.com"} email) :bad-password
            :else :no-user))))

