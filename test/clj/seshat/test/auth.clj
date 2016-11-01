(ns seshat.test.auth
  "\"Interface\" to looking up information about users and sessions during tests.
   Currently implemented again auth.impl.fake, may change/become more extensible in future"
  (:require [seshat.auth.impl.fake :as impl]
            [datomic.api :as d]))

(defn clear-users! [] (reset! impl/users []))

(def sessions
  (reify clojure.lang.IDeref
    (deref [_]
      (let [[sessions] (d/q '[:find (pull ?e [*]) :where [?e :session/id]]
                            (d/db (d/connect "datomic:mem://auth")))]
        (into { } (for [[id [session]] (group-by :session/id sessions)]
                    [(str id) session]))))))


(defn clear-sessions! []
  (let [conn (d/connect "datomic:mem://auth")]
    (doseq [[_ session] @sessions]
      @(d/transact conn [(assoc session :session/expires #inst "1970")]))))

(defn clear! [] (clear-sessions!) (clear-users!))


(def users impl/users)
