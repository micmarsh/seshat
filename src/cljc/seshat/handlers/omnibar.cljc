(ns seshat.handlers.omnibar
  (:require [clojure.string :as str]
            [seshat.lib.notes :as n]
            [re-frame.core :as re-frame]
            [re-frame.handlers :refer [reg-event-re-dispatch]]))

(defn words [text] (str/split text #" "))

(defn only-hashtags? [text]
  (every? #(.startsWith % "#") (words text)))

(defn editing [db]
  (:display/currently-editing (:data/display db)))

(defn search? [text]
  (.startsWith text "search "))

(defn search-term [text]
  (apply str (drop (count "search ") text)))

(defn clear-search? [text]
  (= text "clear search"))

(defn clear-tags? [text]
  (= text "clear tags"))

(defn clear-all? [text]
  (= text "clear"))

(reg-event-re-dispatch
 :omnibar-dispatch
 (fn [{:keys [db]} [_ raw-text]]
   (let [text (str/trim raw-text)]
     (cond (search? text) {:search (search-term text)}
           (clear-search? text) {:search ""}
           (clear-tags? text) (mapv (partial vector :click-tag)
                                    (:filters/tags (:display/filters (:data/display db))))
           (clear-all? text) [[:omnibar-dispatch "clear tags"] [:omnibar-dispatch "clear search"]]
           (only-hashtags? text) (mapv (partial vector :click-tag) (n/tags {:text text}))
           (boolean (editing db)) [[:edited-note text (editing db)] [:cancel-editing-note]]
           :else {:new-note text}))))

