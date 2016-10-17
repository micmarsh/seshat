(ns seshat.session.middleware
  (:require [seshat.session
             [protocols :as p]
             [request :as r]]))

(defn wrap-session
  "Too complected? Too bad. Just deal with it for nows"
  [handler storage]
  (fn [request]
    (if-let [id (r/header request)]
      (if-let [session (p/lookup-session storage id)]
        (-> request
            (assoc :seshat/session session)
            (handler))
        {:status 403
         :headers {}
         :body "Unauthorized session\n"})
      {:status 400
       :headers {}
       :body "need session header\n"})))
