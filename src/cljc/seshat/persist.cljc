(ns seshat.persist)

#?(:clj
   (do (def fake-storage (atom {}))
       (defn clear! [] (reset! fake-storage {}))))

(defn persist-local!
  [key value]
  (assert (string? value)) ;; not the most extensible, too bad
  #?(:cljs (.setItem js/localStorage key value)
     :clj (swap! fake-storage assoc key value)))

(defn fetch-local
  [key]
  #?(:cljs (.getItem js/localStorage key)
     :clj (get @fake-storage key)))

(defn delete-local!
  [key]
  #?(:cljs (.removeItem js/localStorage key)
     :clj (swap! fake-storage dissoc key)))
