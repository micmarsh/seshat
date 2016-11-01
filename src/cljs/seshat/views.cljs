(ns seshat.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
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
           :on-key-down #(when (util/enter? %)
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
  (let [editing? (re-frame/subscribe [:currently-editing])]
    (fn []
      [:div.note-buttons
       (when-not @editing?
         [:button {:on-click #(re-frame/dispatch [:start-editing-note note])}
          "edit"])
       " "
       [:button {:on-click #(re-frame/dispatch [:delete-note note])}
        "delete"]])))

(def separate-inputs
  [:div#inputs
   [new-note-box]
   [editing-note-box]
   [search-box]])

(defn omnibar []
  (let [currently-editing (re-frame/subscribe [:currently-editing])
        dispatch #(when (and (util/enter? %)
                             (not (empty? (util/input-text %))))
                    (re-frame/dispatch [:omnibar-dispatch (util/input-text %)])
                    (util/clear-input %))]
    (fn []
      [:div#omnibar
       (if (nil? @currently-editing)
         [:textarea {:on-key-up dispatch}]
         [:div#editing-omnibar
          [:textarea
           {:on-key-down dispatch
            :default-value (:text @currently-editing)}]
          " "
          [:button {:on-click #(re-frame/dispatch [:cancel-editing-note])} "cancel"]])])))

(defn notes-list []
  (let [notes (re-frame/subscribe [:notes-list])
        omnibar? (r/atom true)]
    (fn []
      [:div#notes-list
       [:h2 "Notes"]
       (if @omnibar?
         [:div
          [:button {:on-click #(do (reset! omnibar? false) false)} "use seperate inputs"]
          [omnibar]]
         [:div
          [:button {:on-click #(do (reset! omnibar? true) false)} "use omnibar"]
          separate-inputs])
       (doall
        (for [note @notes]
          [:div.note-content {:key (notes/id note)}
           (util/display-note (:text note))
           [note-buttons note]]))])))

(defn tags-list []
  (let [tags (re-frame/subscribe [:tags-list])
        selected (re-frame/subscribe [:selected-tags])
        search (re-frame/subscribe [:current-search-term])]
    (fn []
      [:div#tags-list
       [:h2 "Tags"]
       [:div#selected-tags "Selected: "
        (if (empty? @selected)
          "(none)"
          (into ()
                (mapcat (fn [tag] [" " (util/tag-span tag)]))
                @selected))]
       (when-not (empty? @search)
         [:div#search-term "Current Search: " @search])
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

(defn text-bind-callback [atom]
  (fn [event]
    (->> event
         (util/input-text)
         (reset! atom))))

(defn login-form
  []
  (let [fail? (re-frame/subscribe [:failed-login?])
        name (atom "")
        pw (atom "")]
    (fn []
      [:div#login-form
       [:h3 "Login"]
       [:label {:for "name"} "Username or Email"] [:br]
       [:input {:type "text" :on-change (text-bind-callback name)}] [:br]
       [:label {:for "password"} "password"] [:br]
       [:input {:type "password" :on-change (text-bind-callback pw)}] [:br]
       [:button {:on-click #(re-frame/dispatch [:user-login @name @pw])} "login"]
       (when @fail?
         [:div [:span#login-failure-message "Bad credentials"]])])))

(defn register-form
  []
  (let [name (atom "")
        pw (atom "")]
    (fn []
      [:div#login-form 
       [:h3 "Register"] 
       [:label {:for "name"}  "Username or Email"] [:br]
       [:input {:type "text" :on-change (text-bind-callback name)}] [:br]
       [:label {:for "password"} "password"] [:br]
       [:input {:type "password" :on-change (text-bind-callback pw)}] [:br]
       [:button {:on-click #(re-frame/dispatch [:user-register @name @pw])} "register"]])))


(defn not-authed
  []
  (let [view-switch (r/atom :login)]
    (fn []
      (case @view-switch
        :login [:div [login-form]
                [:br]
                [:span {:on-click #(reset! view-switch :register)}
                 "register here"]]
        :register [:div [register-form]
                   [:br]
                   [:span {:on-click #(reset! view-switch :login)}
                    "log in here"]]))))

(defn main-app
  []
  [:div#main-app
   [notes-list]
   [tags-list]
   [uploader]
   [:br] [:button {:on-click #(re-frame/dispatch [:logout])} "Log Out"]])

(defn main-panel
  []
  (let [logged-in? (re-frame/subscribe [:logged-in?])]
    (fn []
      [:div#main-panel
       (if @logged-in?
         [main-app]
         [not-authed])])))
