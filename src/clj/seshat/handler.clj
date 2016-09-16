(ns seshat.handler
  (:require [compojure.core :refer [GET POST PUT DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.edn :refer [wrap-edn-params]]))

(def ^:const fake-data
  [{:id 1
    :text "#todo use some real data"}
   {:id 2
    :text "#music #wewlad Beethoven all the symphonies"}
   {:id 3
    :text "remember #todo stuff other than this side project #today"}
   {:id 4 :text "yes pleas"}
   {:id 5 :text "#wewlad this is a cool app"}])

(def fake-database (atom fake-data))

(def fake-id-gen (atom (:id (last fake-data))))

(defroutes routes*
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/query" [] {:body (pr-str @fake-database)
                    :status 200
                    :headers {"content-type" "application/edn"}})
  (POST "/command/new_note" [:as request]
        (let [new-note (:params request)]
          (locking fake-database
            (if (every? (partial contains? new-note) [:temp-id :text])
              (let [with-id (assoc new-note :id (swap! fake-id-gen inc))]
                (swap! fake-database conj with-id)
                {:body (prn-str with-id)
                 :status 201
                 :headers {"content-type" "application/edn"}})
              {:body "ur data is junk\n"
               :status 400}))))
  (PUT "/command/edit_note/:id" [id :as request]
       (locking fake-database
         (if (contains? (:edn-params request) :text)
           (if-let [existing (first (filter (comp #{id} str :id) @fake-database))]
             (let [updated (assoc existing :text (:text (:edn-params request)))]
               (swap! fake-database (fn [data]
                                      (->> data
                                           (remove (comp #{id} str :id))
                                           (cons updated)
                                           (vec))))
               {:status 200
                :body (prn-str updated)
                :headers {"content-type" "application/edn"}})
             {:status 404
              :body "that stuff doesn't exist\n"})
           {:status 400
            :body "ur data sux\n"})))
  (DELETE "/command/delete_note/:id" [id :as request]
          (locking fake-database
            (if (some? (first (filter (comp #{id} str :id) @fake-database)))
              (do
                (swap! fake-database (fn [data]
                                     (->> data
                                          (remove (comp #{id} str :id))          
                                          (vec))))
                {:status 200
                 :body (prn-str {:deleted 1})
                 :headers {"content-type" "application/edn"}})
              {:status 404
               :body "that stuff doesn't exist, maybe u already deleted?\n"})))
  (resources "/"))

(def routes (wrap-edn-params routes*))

(def dev-handler (-> #'routes wrap-reload))

(def handler routes)
