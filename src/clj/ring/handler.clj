(ns ring.handler
  (:refer-clojure :exclude [compile]))

(defmulti compile type)

(defmethod compile clojure.lang.IFn [h] h)

(defn wrap-handler [handler middleware]
  (if (vector? middleware)
    (apply (first middleware) handler (rest middleware))
    (middleware handler)))

(defmethod compile clojure.lang.APersistentMap
  [{:keys [middleware handler]
    :or {middleware []}}]
  (assert (some? handler))
  (reduce wrap-handler
          (compile handler)
          (reverse middleware)))

(defmethod compile clojure.lang.APersistentVector
  [handlers]
  (let [compiled (mapv compile handlers)]
    (fn [request]
      (some #(% request) compiled))))
