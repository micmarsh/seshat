(ns seshat.handlers.http
  (:require [re-frame.core :as re-frame]))


(def ^:const +special-fail-handler+
  (keyword (gensym (gensym (gensym)))))

(defn auth-failure?
  ;; TODO THIS
  [response] false)

(re-frame/reg-event-fx
 +special-fail-handler+
 (fn [_ [_ {:keys [regular-event auth-event]} & response]]
   (if (auth-failure? response)
     {:dispatch (into auth-event response)}
     {:dispatch (into regular-event response)})))

(defn wrap-auth-failure
  [{:keys [on-failure on-auth-failure] :as request}]
  (cond-> request
    on-auth-failure (assoc :on-failure [+special-fail-handler+
                                        {:regular-event on-failure
                                         :auth-event on-auth-failure}])))

(re-frame/reg-event-fx
 :http
 ;; TODO THESE FUCKIN COFX
 (fn [{:keys [session] :as cofx}
     [_ request]]
   {:http-xhrio (cond-> request
                  session (assoc-in [:headers "session-id"] session)
                  true (wrap-auth-failure))}))

;; TODO CAN TEST THIS SeHIT, SHOULDN'T CHANGE TOO MUCH SINCE WILL JUST
;; not add session and not 
