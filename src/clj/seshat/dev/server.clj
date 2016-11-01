(ns seshat.dev.server
  (:require [ring.middleware.reload :refer [wrap-reload]]
            [seshat.datomic.mem :refer [connection]]
            [seshat.database.impl.datomic :refer [->user-data]]
            [seshat.auth.impl.fake :refer [->auth]]
            [seshat.handler :refer [->routes-data]]
            [ring.handler :as handler]))

(def dev-routes
  (handler/compile
   (->routes-data
    (->user-data @connection)
    (->auth @connection))))

(def handler (-> #'dev-routes wrap-reload))
