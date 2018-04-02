(ns project.components-js
  (:require [rum.core :as rum]
            [ajax.core :refer [GET POST]]
            [project.xml-edit :refer [apply-patterns slurped-xml]]
            [accountant.core :as accountant]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType])
  (:import goog.History
           goog.net.IframeIo
           goog.net.EventType))

(defn get-slurped-xml! []
  (GET "/word-xml"
       {:handler #(reset! slurped-xml %)}))

(rum/defc upload-component < rum/static []
  [:div
   [:form {:id "upload-form"
           :enc-type "multipart/form-data"
           :method "POST"}
    [:label "Upload File: "]
    [:input {:type "file"
             :name "upload-file"
             :id "upload-file"}]]])

(defn cljs-ajax-upload-file [element-id]
  (let [el (.getElementById js/document element-id)
        name (.-name el)
        file (aget (.-files el) 0)
        form-data (doto
                      (js/FormData.)
                    (.append name file))]
    (POST "/upload" {:body form-data
                     :keywords? true
                     :handler #(do (.log js/console (str %))
                                   (get-slurped-xml!))})))

(rum/defc cljs-ajax-upload-button []
  [:div
   [:hr]
   [:button {:type "button"
             :on-click #(cljs-ajax-upload-file "upload-file")}
    "Upload"]])

(rum/defc upload-comp-for-mount < rum/reactive []
  [:div
    (upload-component)
    (cljs-ajax-upload-button)
    [:div.download
     [:a {:href "/edited.docx"} "Download"]]])

