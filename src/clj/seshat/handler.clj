(ns seshat.handler
  (:require [compojure.core :refer [GET POST PUT DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
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

(defroutes routes*
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/query" [] {:body (pr-str @fake-database)
                    :status 200
                    :headers {"content-type" "application/edn"}})
  (POST "/command/new_note" [:as request]
        (let [new-note (:params request)]
          (if (every? (partial contains? new-note) [:temp-id :text])
            (let [note (p/new-note! db (:text new-note))]
              {:body (prn-str (assoc note :temp-id (:temp-id new-note)))
               :status 201
               :headers {"content-type" "application/edn"}})
            {:body "ur data is junk\n"
             :status 400})))
  (PUT "/command/edit_note/:id" [id :as request]
       (locking fake-database
         (if (contains? (:edn-params request) :text)
           (let [id (Integer/parseInt id)]
             (if-let [updated (p/edit-note! db id (:text (:edn-params request)))]
               {:status 200
                :body (prn-str updated)
                :headers {"content-type" "application/edn"}}
               {:status 404
                :body "that stuff doesn't exist\n"}))
           {:status 400
            :body "ur data sux\n"})))
  (DELETE "/command/delete_note/:id" [id :as request]
          (locking fake-database
            (let [id (Integer/parseInt id)
                  deleted (p/delete-note! db id)]
              (if (pos? (:deleted deleted))
                {:status 200
                 :body (prn-str deleted)
                 :headers {"content-type" "application/edn"}}
                {:status 404
                 :body "that stuff doesn't exist, maybe u already deleted?\n"}))))
  (resources "/"))

(def routes (wrap-edn-params routes*))

(def dev-handler (-> #'routes wrap-reload))

(def handler routes)
