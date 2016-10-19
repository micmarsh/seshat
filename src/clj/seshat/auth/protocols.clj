(ns seshat.auth.protocols)

(defprotocol Login
  (login [users email password]
    "Returns a copy of the relevant user object (minus password)
     if credentials are correct, otherwise:
     * :no-user
     * :bad-password
     On relevant error cases"))

(defprotocol Register
  (register! [users email password]
    "If email is not duplicate, creates and returns new user object"))
