(ns seshat.dev.server
  (:require [ring.middleware.reload :refer [wrap-reload]]
            [seshat.database.impl.datomic :refer [initialize]]
            [seshat.auth.impl.fake :refer [fake-auth]]
            [seshat.handler :refer [->routes-data]]
            [ring.handler :as handler]))

(def uri "datomic:mem://fake")

(def dev-routes
  (handler/compile
   (->routes-data
    (initialize uri)
    fake-auth)))

(def handler (-> #'dev-routes wrap-reload))
