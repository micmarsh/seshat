(ns seshat.dev.server
  (:require [ring.middleware.reload :refer [wrap-reload]]
            [seshat.database.impl.fake :refer [fake-user-data]]
            [seshat.auth.impl.fake :refer [fake-auth]]
            [seshat.handler :refer [->routes-data]]
            [ring.handler :as handler]))

(def dev-routes
  (handler/compile
   (->routes-data
    fake-user-data
    fake-auth)))

(def handler (-> #'dev-routes wrap-reload))
