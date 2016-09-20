(ns seshat.handler
  (:require [compojure.core :refer [GET POST PUT DELETE defroutes wrap-routes]]
            [compojure.route :refer [resources]]
            [ring.util.response :as resp]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [seshat.protocols :as p]

            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure
             [string :as str]
             [set :refer [rename-keys]]]))

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
          {:deleted (- (count before) (count @fake-database))})))
    p/ImportNote
    (import-note! [_ {id :fetchnotes/id :as data}]
      (locking fake-database
        (when (empty? (filter (comp #{id} :fetchnotes/id) @fake-database))
          (let [note (assoc data :id (swap! fake-id-gen inc))]
            (swap! fake-database conj note)
            note))))))

(def db fake-impl)

;; General purpose Middleware: TODO: move to new ns, appropriate
;; naming is pretty obvi.
(defn wrap-edn-response [handler]
  (fn [r]
    (some-> (handler r)
            (update :body prn-str)
            (resp/content-type "application/edn"))))

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
  "TODO this is junk, should be handled at some other layer, shareable between query and commands"
  [handler keys]
  (fn [r]
    (some-> (handler r)
            (update :body keep-keys keys))))

(defn wrap-cast-id [handler]
  (fn [req]
    (cond-> req
      (string? (:id (:params req))) (update-in [:params :id] #(Integer/parseInt %))
      true (handler))))

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
         (if-let [updated (p/edit-note! db id text)]
           (resp/response updated)
           (resp/not-found "that stuff doesn't exist"))
         (bad-request "ur data sux")))
  
  (DELETE "/command/delete_note/:id" [id]
          (let [deleted (p/delete-note! db id)]
            (if (pos? (:deleted deleted))
              (resp/response deleted)
              (resp/not-found "that stuff doesn't exist, maybe u already deleted?")))))

(defroutes index-route
  (GET "/" [] (resp/resource-response "index.html" {:root "public"})))

;; Importer stuff, likely headed on over to "fechnotes" branded ns
(defn extension [name]
  (some-> name
          (str/split #"\.")
          (last)))

(def dispatch-read-file (comp extension :filename))

(defmulti read-file dispatch-read-file )

(defmethod read-file "json"
  [{file :tempfile}]
  (-> file
      (io/reader)
      (json/parse-stream true)))

(defn text->map [text]
  (into { }
        (for [entry (str/split text #"\n")
              :let [[key & entry] (str/split entry #": ")]]
          [(keyword key)
           ;; uses json to un-double-encode strings
           (json/decode (str/join ": " entry))])))

(defmethod read-file "txt"
  [{file :tempfile}]
  (-> file
      (slurp)
      (str/split #"\n\n")
      ((partial map text->map))))

(defmethod read-file :default [data]
  (throw (ex-info "Unrecognized filename extension"
                  {:data data
                   :dispatch-result (dispatch-read-file data)})))

;; Importer stuff: pure data manip, no file/reader business
(defn convert-keys [fetch-note]
  (-> fetch-note
      (select-keys [:_id :text :timestamp :created_at])
      (rename-keys {:_id :fetchnotes/id
                    :timestamp :updated
                    :created_at :created})))

(def ts-format
  (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSX"))

(defn parse-timestamp [timestamp]
  (if (some? timestamp)
    (.parse ts-format timestamp)
    ;; should log when that happens or something
    (now)))

(defn parse-timestamps [fetch-note]
  (into { }
        (for [[k v] fetch-note]
          [k (if (#{:created :updated} k)
               (parse-timestamp v)
               v)])))

(defroutes import-routes
  (POST "/import/fetchnotes" [upload-file :as r]
        (try
          (let [notes (->> upload-file
                           (read-file)
                           (map convert-keys)
                           (map parse-timestamps)
                           (keep (partial p/import-note! db)))]
            (resp/response (format "Yay imported %d notes" (count notes))))
          (catch clojure.lang.ExceptionInfo ex
            (bad-request (str "Some problem " (prn-str (ex-data ex))))))))

(def ^:const allowed-response-keys
  ;; TODO this belongs elsewhere as well, not http-layer at all
  [:id :temp-id :text :created :updated :deleted])

(defroutes routes
  index-route
  (-> note-routes
      (wrap-routes wrap-cast-id)
      wrap-edn-params
      (wrap-clean-response allowed-response-keys)
      wrap-edn-response)
  (-> import-routes
      wrap-multipart-params
      (wrap-clean-response allowed-response-keys))
  (resources "/"))

(def dev-handler (-> #'routes wrap-reload))

(def handler routes)
