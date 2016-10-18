(ns seshat.persist)

(defn persist-local!
  [key value]
  (assert (string? value)) ;; not the most extensible, too bad
  (.setItem js/localStorage key value))

(defn fetch-local
  [key]
  (.getItem js/localStorage key))

(defn delete-local!
  [key]
  (.removeItem js/localStorage key))
