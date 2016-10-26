(ns seshat.server
  (:require [seshat.handler :refer [->handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]

            ;; these are going to change before actually deployed
            [seshat.database.impl.fake :refer [fake-user-data]]
            [seshat.auth.impl.fake :refer [fake-auth]])
  (:gen-class))

 (defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3000"))]
     (-> (->handler fake-user-data fake-auth)
         (run-jetty {:port port :join? false}))))
