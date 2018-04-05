(ns project.components-js
  (:require [rum.core :as rum]
            [reagent.core :as r]
            [ajax.core :refer [GET POST]]
            [project.xml-edit :refer [apply-patterns slurped-xml]]
            [accountant.core :as accountant]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType])
  (:import goog.History
           goog.net.IframeIo
           goog.net.EventType))

(def document (r/atom nil))

(defn text [text wtid]
  [:div.wt
   {:on-click #(swap! document
                      (fn [doc] (remove (fn [el] (= (get el "wtid") wtid))
                                        doc)))}
   [:span.text text]])

(defn paragraph [items]
  [:div.line
   {:style {:padding "5px"
            :padding-left "20px"
            :padding-right "20px"}}
   [:div.paragraph
    (for [{wtid "wtid" t "text"} items]
      ^{:key wtid} [text t wtid])]])

(defn get-slurped-xml! []
  (GET "/word-xml"
       {:handler #(reset! document %)}))

(defn upload-component []
  [:div.upload
    [:label "Upload File: "]
    [:input {:type "file"
             :name "upload-file"
             :id "upload-file"
             :on-change (fn [event]
                          (let [name (.-name (.-target event))
                                file (aget (.-files (.-target event)) 0)
                                form-data (doto
                                              (js/FormData.)
                                            (.append name file))]
                            (POST "/upload" {:body form-data
                                             :keywords? true
                                             :handler #(do (.log js/console (str %))
                                                           (get-slurped-xml!))})))}]])

(defn board []
  (when-let [doc @document]
    [:div.board
       [:div.main
        (for [[wpid items] (group-by (fn [v] (get v "wpid")) doc)]
          ^{:key wpid} [paragraph items])]]))

(defn upload-comp-for-mount []
  [:div
    [upload-component]
    #_[:div.download
       [:a {:href "/edited.docx"} "Download"]]
    [board]])

