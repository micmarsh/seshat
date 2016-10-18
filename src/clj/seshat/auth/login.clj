(ns seshat.auth.login
  (:require [seshat.session.protocols :as sp]
            [seshat.auth.protocols :as p]))

(defn new-fake-session [] (str (Math/abs (hash (rand)))))

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
      (let [session-id (new-fake-session)]
        (sp/save-session! storage session-id user)
        {:session session-id}))))
;; This makes a few assumptions you might not want
;; * fake session gen (duh) (should probably be own proto)
;; * login error as keyword (prolly at least hide behind some fn)
