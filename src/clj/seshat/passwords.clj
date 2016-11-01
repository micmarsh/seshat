(ns seshat.passwords
  "Stolen from https://github.com/cemerick/friend/blob/e23d36e043bc0799a544455236ca2e34d8e0e648/src/cemerick/friend/credentials.clj"
  (:import org.mindrot.jbcrypt.BCrypt))

(defn hash-bcrypt
  "Hashes a given plaintext password using bcrypt and an optional
   :work-factor (defaults to 10 as of this writing)."
  [password & {:keys [work-factor]}]
  (BCrypt/hashpw password (if work-factor
                            (BCrypt/gensalt work-factor)
                            (BCrypt/gensalt))))

(defn bcrypt-verify
  "Returns true if the plaintext [password] corresponds to [hash],
the result of previously hashing that password."
  [password hash]
  (BCrypt/checkpw password hash))
