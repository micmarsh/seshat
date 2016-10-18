(ns seshat.session.request)

(defn header [request]
  (get-in request [:headers "session-id"]))
