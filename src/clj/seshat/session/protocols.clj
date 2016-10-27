(ns seshat.session.protocols)

(defprotocol NewFromUser
  (from-user [store user]
    "Given a user (usually returns from auth/register! or auth/login), create a session
     suitable for saving in the given storage medium (see below)"))

(defprotocol UserId
  (user-id [store session] "Return a session's user's id."))

(defprotocol Id
  (id [store session] "Return a session's own id"))

(defprotocol Lookup
  (lookup-session [store key]
    "Returns a session or nil if no such record exists"))

(defprotocol Save
  (save-session! [store session]
    "Saves a new session to the given storage medium. 
     Session is expected to already contain some unique identifer (see Id above)"))
