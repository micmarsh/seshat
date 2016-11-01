(ns seshat.dev.server
  (:require [ring.middleware.reload :refer [wrap-reload]]
            [seshat.datomic.mem :refer [connection]]
            [seshat.database.impl.datomic :refer [->user-data]]
            [seshat.auth.impl.datomic :refer [->auth]]
            [seshat.handler :refer [->routes-data]]
            [ring.handler :as handler]))

(def auth (->auth @connection))

(def dev-routes
  (handler/compile
   (->routes-data
    (->user-data @connection)
    auth)))

(def handler (-> #'dev-routes wrap-reload))
