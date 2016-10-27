(ns seshat.spec.client
  (:require [clojure.spec :as s]
            [seshat.spec.notes]))

(s/def :data/notes (s/coll-of :note/text-only))

(s/def :data/auth (s/keys :req [:auth/session-id
                                :auth/login-fail]))
(s/def :auth/session-id (s/nilable string?))
(s/def :auth/login-fail boolean?)

(s/def :data/display
  (s/keys :req [:display/notes
                :display/tags
                :display/filters
                :display/currently-editing
                :display/currently-uploading
                :display/upload-error]))
(s/def :display/notes (s/coll-of :note/text-only))
(s/def :display/tags (s/coll-of string?))

;; TODO these can probably be moved out of "client", utilize in future
;; "query language" on backend
(s/def :filters/tags (s/and set? (s/coll-of string?)))
(s/def :filters/search string?)

(s/def :display/filters (s/keys :req [:filters/tags :filters/search]))
(s/def :display/currently-editing (s/nilable :note/text-only))
(s/def :display/currently-uploading boolean?)
(s/def :display/upload-error boolean?)

(s/def :client/db
  (s/keys :req [:data/notes
                :data/auth
                :data/display]))
