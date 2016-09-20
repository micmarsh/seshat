(ns seshat.handler
  (:require [compojure.core :refer [GET POST PUT DELETE defroutes wrap-routes]]
            [compojure.route :refer [resources]]
            [ring.util.response :as resp]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [seshat.database.protocols :as p]
            [seshat.database.impl.fake :refer [fake-impl]]
            [seshat.middleware :as m]
            [seshat.import.fetchnotes :as f]))

(def db fake-impl)

(def bad-request (partial hash-map :status 400 :body))

(defroutes note-routes
  (GET "/query" [] (resp/response (p/query db {})))

  (POST "/command/new_note" [temp-id text]
        (if (and (some? temp-id) (some? text))
          (let [note (p/new-note! db text)
                result (assoc note :temp-id temp-id)]
            (resp/created "/command/new_note" result))
          (bad-request "ur data is junk")))
  
  (PUT "/command/edit_note/:id" [id text]
       (if (some? text)
         (if-let [updated (p/edit-note! db id text)]
           (resp/response updated)
           (resp/not-found "that stuff doesn't exist"))
         (bad-request "ur data sux")))
  
  (DELETE "/command/delete_note/:id" [id]
          (let [deleted (p/delete-note! db id)]
            (if (pos? (:deleted deleted))
              (resp/response deleted)
              (resp/not-found "that stuff doesn't exist, maybe u already deleted?")))))

(defroutes import-routes
  (POST "/import/fetchnotes" [upload-file :as r]
        (try
          (let [notes (keep (partial p/import-note! db)
                            (f/extract-notes upload-file))]
            (resp/response notes))
          (catch clojure.lang.ExceptionInfo ex
            (bad-request (str "Some problem " (prn-str (ex-data ex))))))))

(def ^:const allowed-response-keys
  ;; TODO this belongs elsewhere as well, not http-layer at all
  [:id :temp-id :text :created :updated :deleted])

(defroutes routes
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (-> note-routes
      (wrap-routes m/wrap-cast-id)
      (wrap-edn-params)
      (m/wrap-clean-response allowed-response-keys)
      (m/wrap-edn-response))
  (-> import-routes
      (wrap-multipart-params)
      (m/wrap-clean-response allowed-response-keys)
      (m/wrap-edn-response))
  (resources "/"))

(def dev-handler (-> #'routes wrap-reload))

(def handler routes)
