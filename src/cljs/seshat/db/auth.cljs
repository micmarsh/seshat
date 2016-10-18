(ns seshat.db.auth)

(defn get-session [db]
  (-> db :data/auth :auth/session-id))

(defn set-session [db session-id]
  (assoc-in db [:data/auth :auth/session-id] session-id))
