(ns seshat.handler
  (:require [compojure.core :refer [GET POST PUT DELETE defroutes wrap-routes]]
            [compojure.route :refer [resources]]
            [ring.handler :as handler]
            [ring.util.response :as resp]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [seshat.database.protocols :as p]
            [seshat.database.impl.fake :refer [fake-user-data]]
            [seshat.auth.impl.fake :refer [fake-auth]]
            [seshat.auth.login :as auth] ;; TODO ns name change
            [seshat.middleware :as m]
            [seshat.session.middleware :as sm]
            [seshat.import.fetchnotes :as f]))

(def bad-request (partial hash-map :status 400 :body))

(def query-route
  (GET "/query" [:as r] (resp/response (p/query (:db r) {}))))

(def new-note-route
  (POST "/command/new_note" [temp-id text :as r]
        (if (and (some? temp-id) (some? text))
          (let [note (p/new-note! (:db r) text)
                result (assoc note :temp-id temp-id)]
            (resp/created "/command/new_note" result))
          (bad-request "ur data is junk"))))

(defroutes resource-command-routes
  (PUT "/command/edit_note/:id" [id text :as r]
       (if (some? text)
         (if-let [updated (p/edit-note! (:db r) id text)]
           (resp/response updated)
           (resp/not-found "that stuff doesn't exist"))
         (bad-request "ur data sux")))
  
  (DELETE "/command/delete_note/:id" [id :as r]
          (let [deleted (p/delete-note! (:db r) id)]
            (if (pos? (:deleted deleted))
              (resp/response deleted)
              (resp/not-found "that stuff doesn't exist, maybe u already deleted?")))))

(def import-route
  (POST "/import/fetchnotes" [upload-file :as r]
        (let [notes (keep (partial p/import-note! (:db r))
                          (f/extract-notes upload-file))]
          (resp/response notes))))

(def login-route
  (POST "/login" [email password]
        (let [login-result (auth/session-login! fake-auth email password)]
          (if (keyword? login-result) ;; that darn keyword check again
            {:status 401 :body {:email email :password password} :headers {}}
            (resp/response login-result)))))

(def register-route
  (POST "/register" [email password]
        (if-let [register-result (auth/session-register! fake-auth email password)]
          (resp/response register-result)
          (bad-request "couldn't register"))))

(def ^:const allowed-response-keys
  ;; TODO this belongs elsewhere as well, not http-layer at all
  [:id :temp-id :text :created :updated :deleted])

(def note-routes
  {:middleware [[sm/wrap-session fake-auth]
                [sm/wrap-user-data fake-user-data]
                [m/wrap-clean-response allowed-response-keys]]
   :handler [new-note-route
             query-route
             {:middleware [[wrap-routes m/wrap-cast-id]]
              :handler resource-command-routes}]})

(def import-route
  {:middleware [[sm/wrap-session fake-auth]
                [sm/wrap-user-data fake-user-data]
                wrap-multipart-params]
   :handler import-route})

(def edn-routes
  "Best to organize this way due to mutable stream action in param parsing"
  {:middleware [wrap-edn-params]
   :handler [login-route
             register-route
             note-routes]})

(def routes-data
  [(GET "/" [] (resp/resource-response "index.html" {:root "public"}))
   {:middleware [m/wrap-edn-response]
    :handler [edn-routes import-route]}
   (resources "/")])

(def routes (handler/compile routes-data))

(def dev-handler (-> #'routes wrap-reload))

(def handler routes)
