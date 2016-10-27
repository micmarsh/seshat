(ns seshat.middleware
  (:require [ring.util.response :as resp]
            [clojure.spec :as s]))

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

(defn wrap-validate-params [handler spec]
  (fn [{:keys [params] :as request}]
    (if-let [error (s/explain-data spec params)]
      {:status 400
       :headers {}
       :body error}
      (handler request))))
