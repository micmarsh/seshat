(ns seshat.auth.user
  (:require [seshat.session.protocols :as sp]
            [seshat.auth.protocols :as p]))

(defn session-login!
  "Given properly implemented storage
   * checks user creds
   * creates + stores new session if valid
   * returns {:session \"session-id\"} if valid
   * forwards Login error kw otherwise"
  [storage email password]
  (let [user (p/login storage email password)]
    (if (keyword? user)
      user
      (let [new-session (sp/from-user storage user)]
        (sp/save-session! storage new-session)
        {:session (sp/id storage new-session)}))))
;; This makes a few assumptions you might not want
;; * fake session gen (duh) (should probably be own proto)
;; * login error as keyword (prolly at least hide behind some fn)

(defn session-register!
  "Given properly implemented storage, creates new user if able
   then returns a new session"
  [storage email password]
  (when-let [user (p/register! storage email password)]
    (let [new-session (sp/from-user storage user)]
      (sp/save-session! storage new-session)
      {:session (sp/id storage new-session)})))
