(ns seshat.session.middleware
  (:require [seshat.session
             [protocols :as p]
             [request :as r]]
            [seshat.database.protocols :as db]))

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
      {:status 403
       :headers {}
       :body "need session header\n"})))

(defn wrap-user-data
  [handler db]
  (fn [request]
    (let [session-user (:seshat/session request)
          user-id (:id session-user)]
      (-> request
          (assoc :db (db/user-filter db user-id))
          (handler)))))
