(ns seshat.test.auth
  "\"Interface\" to looking up information about users and sessions during tests.
   Currently implemented again auth.impl.fake, may change/become more extensible in future"
  (:require [seshat.auth.impl.datomic :as impl]
            [seshat.datomic.mem :refer [connection]]
            [datomic.api :as d]))

(def users
  (reify clojure.lang.IDeref
    (deref [_]
      (let [[users] (d/q '[:find (pull ?e [*]) :where [?e :user/deleted? false]]
                         (d/db @connection))]
        users))))

(defn clear-users! []
  (doseq [user @users]
    @(d/transact @connection [(assoc user :user/deleted? true)])))

(def sessions
  (reify clojure.lang.IDeref
    (deref [_]
      (let [sessions (d/q '[:find (pull ?e [*]) :where [?e :session/id]]
                          (impl/active-sessions (d/db @connection)))]
        (into { } (for [[id [session]] (group-by :session/id (map first  sessions))]
                    [(str id) session]))))))


(defn clear-sessions! []
  (doseq [[_ session] @sessions]
    @(d/transact @connection [(assoc session :session/expires #inst "1970")])))

(defn clear! [] (clear-sessions!) (clear-users!))

