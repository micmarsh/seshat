(ns seshat.database.protocols)

(defprotocol NewNote
  (new-note! [db text]
    "Given a datastore and saves a new note to the datastore, returns
      * :text (the text provided)
      * :id (a new unique identifier for the note)
      * :created, :updated (some timestamp stuff) (specify later)"))

(defprotocol ReadNote
  (read-note [db id]
    "Read note from datastore for given id, returns nil if not found"))

(defprotocol EditNote
  (edit-note! [db id text]
    "Given a datastore, note id, and new text, updates the text of the given note (by id), returns
     nil if no such note by id exists, otherwise
      * :text (the text provided)
      * :id (the same id provided)
      * :updated (changed to current time)
      * :created (unchanged, never changes)"))

(defprotocol DeleteNote
  (delete-note! [db id]
    "Deletes given (by id) note. returns
      * :deleted (1 or 0, depending on success of delete)"))

(defprotocol ImportNote
  (import-note! [db data]
    "Assigns a new id to the given data and stores it in the given database.
     Returns same result as NewNote, or nil if can determine note has already been
     imported"))

(defprotocol QueryNotes
  (query [db query]))

(defprotocol UserFilter
  (user-filter [db user-id]
    "Returns an implementation of one or more of the above, restricted to only affecting
     the given users' dataset"))
