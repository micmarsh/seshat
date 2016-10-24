(ns seshat.handlers.http
  (:require [re-frame.core :as re-frame]))


(def ^:const +special-fail-handler+
  (keyword (gensym (gensym (gensym)))))

(def auth-failure? (comp #{401 403} :status))

(re-frame/reg-event-fx
 +special-fail-handler+
 (fn [_ [_ {:keys [regular-event auth-event]} response]]
   {:dispatch (conj (if (auth-failure? response)
                      auth-event
                      regular-event)
                    response)}))

(defn wrap-auth-failure
  [{:keys [on-failure on-auth-failure] :as request}]
  (cond-> request
    on-auth-failure (assoc :on-failure [+special-fail-handler+
                                        {:regular-event on-failure
                                         :auth-event on-auth-failure}])))

(re-frame/reg-event-fx
 :http
 (re-frame/inject-cofx :session)
 (fn [{:keys [session] :as cofx} [_ request]]
   {:http-xhrio (cond-> request
                  session (assoc-in [:headers "session-id"] session)
                  true (wrap-auth-failure))}))
