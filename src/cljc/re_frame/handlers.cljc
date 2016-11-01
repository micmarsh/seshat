(ns re-frame.handlers
  (:require [re-frame.core :refer [reg-event-fx]]))

(defn reg-event-re-dispatch
  ([event-key handler] (reg-event-re-dispatch event-key [] handler))
  ([event-key middleware handler]
   (reg-event-fx
    event-key
    middleware
    (fn [& args]
      (if-let [events (seq (apply handler args))]
        {:dispatch-n events}
        {})))))
