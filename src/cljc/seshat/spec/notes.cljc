(ns seshat.spec.notes
  (:require [clojure.spec :as s]))

(s/def :note/text string?)
(s/def :note/id int?)
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
  {:id 1
   :temp-id 'temp
   :text "sample"
   :created #inst "2016-01-01"
   :updated #inst "2016-01-01"})

(assert (s/valid? :note/full sample) "sample is up to date")

(def ^:const spec-keys (keys sample))

(defn trim [note] (select-keys note spec-keys))
