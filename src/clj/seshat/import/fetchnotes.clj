(ns seshat.import.fetchnotes
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure
             [string :as str]
             [set :refer [rename-keys]]]))

(defn extension [name]
  (some-> name
          (str/split #"\.")
          (last)))

(def dispatch-read-file (comp extension :filename))

(defmulti read-file dispatch-read-file)

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
    (java.util.Date.)))

(defn parse-timestamps [fetch-note]
  (into { }
        (for [[k v] fetch-note]
          [k (if (#{:created :updated} k)
               (parse-timestamp v)
               v)])))

(defn extract-notes [upload-file]
  ;; Sloppy throwing together of things, likely going to get
  ;; separated once this saves more info to a real database
  (->> upload-file
       (read-file)
       (map convert-keys)
       (map parse-timestamps)))
