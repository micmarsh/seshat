(ns seshat.handler
  (:require [compojure.core :refer [GET POST PUT DELETE defroutes wrap-routes]]
            [compojure.route :refer [resources]]
            [ring.handler :as handler]
            [ring.util.response :as resp]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [seshat.database.protocols :as p]
            [seshat.auth.user :as auth]
            [seshat.middleware :as m]
            [seshat.session.middleware :as sm]
            [seshat.import.fetchnotes :as f]
            [seshat.spec.notes]
            [clojure.spec :as s]))

(def bad-request (partial hash-map :status 400 :body))

(def query-route
  (GET "/query" [:as r] (resp/response (p/query (:db r) {}))))

(def new-note-route
  (POST "/command/new_note" [temp-id text :as r]        
        (let [note (p/new-note! (:db r) text)
              result (assoc note :temp-id temp-id)]
          (resp/created "/command/new_note" result))))

(def edit-note-route
  (PUT "/command/edit_note" [id text :as r]
       (if (some? text)
         (if-let [updated (p/edit-note! (:db r) id text)]
           (resp/response updated)
           (resp/not-found "that stuff doesn't exist"))
         (bad-request "ur data sux"))))

(def delete-note-route
  (DELETE "/command/delete_note" [id :as r]
          (let [deleted (p/delete-note! (:db r) id)]
            (if (pos? (:deleted deleted))
              (resp/response deleted)
              (resp/not-found "that stuff doesn't exist, maybe u already deleted?")))))

(def import-route
  (POST "/import/fetchnotes" [upload-file :as r]
        (let [notes (keep (partial p/import-note! (:db r))
                          (f/extract-notes upload-file))]
          (resp/response notes))))

(defn ->login-route [auth]
  (POST "/login" [email password]
        (let [login-result (auth/session-login! auth email password)]
          (if (keyword? login-result) ;; that darn keyword check again
            {:status 401 :body {:email email :password password} :headers {}}
            (resp/response login-result)))))

(defn ->register-route [auth]
  (POST "/register" [email password]
        (if-let [register-result (auth/session-register! auth email password)]
          (resp/response register-result)
          (bad-request "couldn't register"))))

(def new-note-params (s/keys :req-un [:note/text :note/temp-id]))

(def edit-note-params (s/keys :req-un [:note/text :note/id]))

(def delete-note-params (s/keys :req-un [:note/id]))

(def resource-command-routes
  [{:middleware [[wrap-routes m/wrap-validate-params edit-note-params]]
    :handler edit-note-route}
   {:middleware [[wrap-routes m/wrap-validate-params delete-note-params]]
    :handler delete-note-route}])

(defn ->note-routes [db auth]
  {:middleware [[sm/wrap-session auth]
                [sm/wrap-user-data db auth]]
   :handler [{:middleware [[wrap-routes m/wrap-validate-params new-note-params]]
              :handler new-note-route}
             query-route
             resource-command-routes]})

(defn ->import-route [db auth]
  {:middleware [[sm/wrap-session auth]
                [sm/wrap-user-data db auth]
                wrap-multipart-params]
   :handler import-route})

(defn ->edn-routes [db auth]
  "Best to organize this way due to mutable stream action in param parsing"
  {:middleware [wrap-edn-params]
   :handler [(->login-route auth)
             (->register-route auth)
             (->note-routes db auth)]})

(defn ->routes-data [db auth]
  [(GET "/" [] (resp/resource-response "index.html" {:root "public"}))
   {:middleware [m/wrap-edn-response]
    :handler [(->edn-routes db auth)
              (->import-route db auth)]}
   (resources "/")])

(defn ->handler [db auth]
  (handler/compile (->routes-data db auth)))
