(ns seshat.test.http
  (:require [seshat.handler :refer [handler]]
            [re-frame.core :as re-frame]
            [clojure.set :refer [rename-keys]]))

(defn ajax->ring-req [ajax]
  (-> ajax
      (update :body read-string) ;; do it easy b/c why not?
      (rename-keys {:method :request-method :body :params})))

(defn ring-resp->ajax [resp]
  (update resp :body read-string))

(defn dispatch-result
  [{:keys [on-success on-failure]} response]
  (if (<= 400 (:status response))
    (re-frame/dispatch (conj on-failure response))
    (re-frame/dispatch (conj on-success (:body response)))))

(defn fake-http-request
  [request]
  (let [resp-handlers (select-keys request [:on-success :on-failure])]
     (->> request
          (ajax->ring-req)
          (handler)
          (ring-resp->ajax)
          (dispatch-result resp-handlers))))

(re-frame/reg-fx :http-xhrio fake-http-request)
