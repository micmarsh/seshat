(ns seshat.test.auth
  "\"Interface\" to looking up information about users and sessions during tests.
   Currently implemented again auth.impl.fake, may change/become more extensible in future"
  (:require [seshat.auth.impl.fake :as impl]))

(defn clear-users! [] (reset! impl/users []))

(defn clear-sessions! [] (reset! impl/sessions { }))

(defn clear! [] (clear-sessions!) (clear-users!))

(def sessions impl/sessions)

(def users impl/users)
