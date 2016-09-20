(ns seshat.views
  (:require [re-frame.core :as re-frame]
            [seshat.lib.notes :as notes]
            [seshat.views.util :as util]))

(defn new-note-box []
  (fn []
    [:div#new-note-entry
     [:span "New Note: "]
     [:textarea
      {:on-key-up #(when (and (util/enter? %)
                              (not (empty? (util/input-text %))))
                     (util/submit-text %))}]]))

(defn editing-note-box []
  (let [currently-editing (re-frame/subscribe [:currently-editing])]
    (fn []
      (when-not (nil? @currently-editing)
        [:div#editing-note
         [:span "Edit Note: "]
         [:textarea
          {:default-value (:text @currently-editing)
           :on-key-up #(when (util/enter? %)
                         (re-frame/dispatch [:edited-note (util/input-text %) @currently-editing])
                         (re-frame/dispatch [:cancel-editing-note]))}]
         " "
         [:button {:on-click #(re-frame/dispatch [:cancel-editing-note])} "cancel"]]))))

(defn search-box []
  (fn []
    [:div#search-box
     [:span "Search: "]
     [:input {:on-change #(re-frame/dispatch [:search (util/input-text %)])}]]))

(defn note-buttons [note]
  (fn []
    [:div.note-buttons
     [:button {:on-click #(re-frame/dispatch [:start-editing-note note])}
      "edit"]
     " "
     [:button {:on-click #(re-frame/dispatch [:delete-note note])}
      "delete"]]))

(defn notes-list []
  (let [notes (re-frame/subscribe [:notes-list])]
    (fn []
      [:div#notes-list
       [:h2 "Notes"]
       [new-note-box]
       [editing-note-box]
       [search-box]
       (doall
        (for [note @notes]
          [:div.note-content {:key (notes/id note)}
           (util/display-note (:text note))
           [note-buttons note]]))])))

(defn tags-list []
  (let [tags (re-frame/subscribe [:tags-list])
        selected (re-frame/subscribe [:selected-tags])]
    (fn []
      [:div#tags-list
       [:h2 "Tags"]
       [:div#selected-tags "Selected: "
        (if (empty? @selected)
          "(none)"
          (into ()
                (mapcat (fn [tag] [" " (util/tag-span tag)]))
                @selected))]
       (doall
        (for [tag @tags]
          (assoc (util/tag-span tag) 0 :div.tag-text)))])))

(defn file-data [element]
  (let [name (.-name element)
        file (aget (.-files element) 0)]
    (doto (js/FormData.) (.append name file))))

(defn upload-file [element-id]
  (let [element (.getElementById js/document element-id)]
    (re-frame/dispatch [:upload-file (file-data element)])
    (set! (.-value element) nil)
    false))

(def ^:const file-element "upload-file")

(defn uploader []
  (let [uploading? (re-frame/subscribe [:currently-uploading])
        error? (re-frame/subscribe [:upload-error])]
    (fn []
      [:div
       [:h2 "Import"]
       (if @uploading?
         [:div#uploading-message "Uploading..."]
         [:div#uploader {}
          (when @error?
            [:div [:span#uploading-error-message "Something broke"]])
          [:form#upload-form {:enc-type "multipart/form-data"}
           [:label "Upload Filename: "]
           [:input
            {:id file-element
             :type "file"
             :name "upload-file"}]]
          [:button {:on-click #(upload-file file-element)} "Upload"]])])))

(defn main-panel []
  [:div#main-panel
   [notes-list]
   [tags-list]
   [uploader]])
