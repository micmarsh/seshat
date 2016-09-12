(ns seshat.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.format :refer [wrap-restful-format]]))

(def ^:const fake-data
  [{:id 1
    :text "#todo use some real data"}
   {:id 2
    :text "#music #wewlad Beethoven all the symphonies"}
   {:id 3
    :text "remember #todo stuff other than this side project #today"}
   {:id 4 :text "yes pleas"}
   {:id 5 :text "#wewlad this is a cool app"}])

(defroutes routes*
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/hello" [] "\"nope\"")
  (GET "/query" [] {:body fake-data
                    :status 200
                    :headers {"content-type" "application/edn"}})
  (resources "/"))

(def routes
  (wrap-restful-format routes* :formats [:edn]))

(def dev-handler (-> #'routes wrap-reload))

(def handler routes)
