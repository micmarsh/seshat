(ns seshat.spec.notes
  (:require [clojure.spec :as s]))

(s/def :note/text string?)
(s/def :note/id uuid?)
(s/def :note/temp-id symbol?)
(s/def :note/created inst?)
(s/def :note/updated inst?)

(s/def :note/text-only
  (s/keys :req-un [:note/text]
          :opt-un [:note/temp-id]))

(s/def :note/full
  (s/merge :note/text-only
           (s/keys :req-un [:note/id :note/created :note/updated])))

(def ^:const sample
  {:id #uuid "a8127d69-b91a-4b90-ba40-938af8a2db26"
   :temp-id 'temp
   :text "sample"
   :created #inst "2016-01-01"
   :updated #inst "2016-01-01"})

(assert (s/valid? :note/full sample) "sample is up to date")

(def ^:const spec-keys (keys sample))

(defn trim [note]
  (let [un-nsed (into {} (for [[k v] note] [(keyword (name k)) v]))]
    (select-keys un-nsed spec-keys)))
