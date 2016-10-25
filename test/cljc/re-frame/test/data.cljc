(ns re-frame.test.data
  (:require [re-frame.core :as rf]))

(defn run-test-data
  "Expects you to have rigged up all dispatches to be synchronous"
  [tests]
  (doall
   (mapcat
    (fn [{:keys [events subscriptions]}]
      (doseq [event-v events]
        (rf/dispatch event-v))
      (for [[sub-key sub-val] subscriptions
            :let [{:keys [transform]
                   :or {transform identity}} (meta sub-key)
                  subscription (rf/subscribe sub-key)]]
        {:expected sub-val
         :actual (transform @subscription)
         :result (= sub-val (transform @subscription))}))
    tests)))


(comment
  [{:events [initialize stuff]}
   
   {:events [things to dispatch]
    :subscriptions {^{:transform yo} [:sub-name1] purest-val
                    [:sub-name2] pure-value}}
   
   {:subscriptions [pre check for below for some reason]}   
   {:events [moar dispatch]
    :subscriptions {[:sub-name1] woo}}]
  
  )

  (comment
    [{:input [[:delete-note a-note]]
      :ouput {:notes-list (some fuckin notes)}}
     {:input [[:edit-note "blah blah" another-note]]
      :output {:notes-list ()}}]

    )
