(ns seshat.session.protocols)

(defprotocol Lookup
  (lookup-session [store key]
    "Returns a session (recognized by user.protocols/read-user), 
     or nil if no such record exists"))
