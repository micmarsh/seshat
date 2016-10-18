(ns seshat.session.protocols)

(defprotocol Lookup
  (lookup-session [store key]
    "Returns a session or nil if no such record exists"))

(defprotocol Save
  (save-session! [store key val]
    "Saves a new session to the given storage medidum"))
