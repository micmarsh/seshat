(ns seshat.datomic.mem
  (:require [datomic.api :as d]
            [seshat.datomic.schema :refer [full]]))

(def ^:const uri "datomic:mem://main-database")

(def connection
  (delay
   (when (d/create-database uri)
     @(d/transact (d/connect uri) full))
   (d/connect uri)))
