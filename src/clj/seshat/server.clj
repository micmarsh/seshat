(ns seshat.server
  (:require [seshat.handler :refer [->handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]

            ;; these are going to change before actually deployed
            [seshat.database.impl.datomic :refer [initialize]]
            [seshat.auth.impl.fake :refer [fake-auth]])
  (:gen-class))

(def uri "datomic:mem://fake")

 (defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3000"))]
     (-> (->handler (initialize uri) fake-auth)
         (run-jetty {:port port :join? false}))))
