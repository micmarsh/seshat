(ns seshat.server
  (:require [seshat.handler :refer [->handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]

            ;; these are going to change before actually deployed
            [seshat.datomic.mem :refer [connection]]
            [seshat.database.impl.datomic :refer [->user-data]]
            [seshat.auth.impl.fake :refer [->auth]])
  (:gen-class))


 (defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3000"))]
     (-> (->handler (->user-data @connection)
                    (->auth @connection))
         (run-jetty {:port port :join? false}))))
