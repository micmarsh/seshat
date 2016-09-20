(ns seshat.middleware
  (:require [ring.util.response :as resp]))

(defn wrap-edn-response [handler]
  (fn [r]
    (some-> (handler r)
            (update :body prn-str)
            (resp/content-type "application/edn"))))

(defn wrap-cast-id [handler]
  (fn [req]
    (cond-> req
      (string? (:id (:params req))) (update-in [:params :id] #(Integer/parseInt %))
      true (handler))))

;; API return sanitization, not the ideal ns for it
(defmulti keep-keys
  (fn [data keys] (type data)))

(defmethod keep-keys clojure.lang.APersistentVector
  [seq keys]
  (mapv #(keep-keys % keys) seq))

(defmethod keep-keys clojure.lang.LazySeq
  [seq keys]
  (map #(keep-keys % keys) seq))

(defmethod keep-keys clojure.lang.APersistentMap
  [map keys]
  (select-keys map keys))

(defmethod keep-keys :default
  [data _]
  (println "no dispatch result for" data)
  data)

(defn wrap-clean-response
  [handler keys]
  (fn [r]
    (some-> (handler r)
            (update :body keep-keys keys))))
