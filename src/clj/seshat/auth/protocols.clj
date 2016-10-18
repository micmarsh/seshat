(ns seshat.auth.protocols)

(defprotocol Login
  (login [users email password]
    "Returns a copy of the relevant user object (minus password)
     if credentials are correct, otherwise:
     * :no-user
     * :bad-password
     On relevant error cases"))
