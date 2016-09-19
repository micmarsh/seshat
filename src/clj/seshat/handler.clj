(ns seshat.handler
  (:require [compojure.core :refer [GET POST PUT DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :as resp]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [seshat.protocols :as p]))


;; TODO this fake data stuff goes somewhere else, get injected
;; somewhere around main fn (and here, due to wrap-reload action, but
;; design for normal DI first)
(def now #(java.util.Date.))

(def ^:const fake-data
  (mapv
   #(assoc % :created (now) :updated (now))
   [{:id 1
     :text "#todo use some real data"}
    {:id 2
     :text "#music #wewlad Beethoven all the symphonies"}
    {:id 3
     :text "remember #todo stuff other than this side project #today"}
    {:id 4 :text "yes pleas"}
    {:id 5 :text "#wewlad this is a cool app"}]))

(def fake-database (atom fake-data))

(def fake-id-gen (atom (:id (last fake-data))))

(def fake-impl
  (reify
    p/NewNote
    (new-note! [_ text]
      (let [note {:text text
                  :id (swap! fake-id-gen inc)
                  :created (now)
                  :updated (now)}]
        (swap! fake-database conj note)
        note))
    p/ReadNote
    (read-note [_ id]
      (first (filter (comp #{id} :id) @fake-database)))
    p/EditNote
    (edit-note! [this id text]
      (locking fake-database
        (when-let [note (p/read-note this id)]
          (let [updated (assoc note :text text :updated (now))]
            (swap! fake-database (fn [data]
                                   (->> data
                                        (remove (comp #{id} :id))
                                        (cons updated)
                                        (vec))))
            updated))))
    p/DeleteNote
    (delete-note! [_ id]
      (locking fake-database
        (let [before @fake-database]
          (swap! fake-database (fn [data]
                                 (->> data
                                      (remove (comp #{id} :id))          
                                      (vec))))
          {:deleted (- (count before) (count @fake-database))})))))

(def db fake-impl)

;; General purpose Middleware: TODO: move to new ns, appropriate
;; naming is pretty obvi.
(defn wrap-edn-response [handler]
  (fn [r]
    (-> (handler r)
        (update :body prn-str)
        (resp/content-type "application/edn"))))

;; function that should be in ring
(def bad-request (partial hash-map :status 400 :body))

(defroutes note-routes
  (GET "/query" [] (resp/response @fake-database))

  (POST "/command/new_note" [temp-id text]
        (if (and (some? temp-id) (some? text))
          (let [note (p/new-note! db text)
                result (assoc note :temp-id temp-id)]
            (resp/created "/command/new_note" result))
          (bad-request "ur data is junk")))

  (PUT "/command/edit_note/:id" [id text]
       (if (some? text)
         (let [id (Integer/parseInt id)]
           (if-let [updated (p/edit-note! db id text)]
             (resp/response updated)
             (resp/not-found "that stuff doesn't exist")))
         (bad-request "ur data sux")))

  (DELETE "/command/delete_note/:id" [id]
          (let [id (Integer/parseInt id)
                deleted (p/delete-note! db id)]
            (if (pos? (:deleted deleted))
              (resp/response deleted)
              (resp/not-found "that stuff doesn't exist, maybe u already deleted?")))))

(defroutes index-route
  (GET "/" [] (resp/resource-response "index.html" {:root "public"})))

(defroutes routes*
  index-route
  (wrap-edn-response note-routes)
  (resources "/"))

(def routes
  (-> routes*
      (wrap-edn-params)))

(def dev-handler (-> #'routes wrap-reload))

(def handler routes)
