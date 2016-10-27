(ns seshat.test.http
  (:require [seshat.dev.server :refer [handler]]
            [re-frame.core :as re-frame]
            [clojure.set :refer [rename-keys]]))

(defn ajax->ring-req [ajax]
  (cond-> ajax
    (:body ajax) (update :body read-string) ;; do it easy b/c why not?
    true (rename-keys {:method :request-method :body :params})))

(defn ring-resp->ajax [resp]
  (cond-> resp
    (:body resp) (update :body read-string)))

(defn dispatch-result
  [{:keys [on-success on-failure]} response]
  (if (<= 400 (:status response))
    (re-frame/dispatch (conj on-failure response)) ;; TODO this might
    ;; not be correct (see below), don't hacare atm
    (re-frame/dispatch (conj on-success (:body response)))))

(defn fake-http-request
  [request]
  (->> request
       (ajax->ring-req)
       (handler)
       (ring-resp->ajax)))

(re-frame/reg-fx
 :http-xhrio
 (fn [request]
   (let [resp-handlers (select-keys request [:on-success :on-failure])]
     (->> request
          (fake-http-request)
          (dispatch-result resp-handlers)))))
