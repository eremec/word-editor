(ns project.handler
  (:require [compojure.core :refer [GET POST defroutes routes]]
            [compojure.route :refer [not-found resources]]
            [ring.util.response :refer [file-response resource-response]]
            [ring.util.http-response :refer [ok]]
            [rum.core :as rum]
            [project.middleware :refer [wrap-home wrap-base]]
            [project.components :as html]
            [project.xml-edit :as xml]
            [project.xml-handler :refer [take-indexed-content update-map update-line update-xml get-root]]
            [project.filesystem  :refer [unzip-file zip-dir]]))

(defn page [component]
  (str
    "<!doctype html>
      <html>
       <head>
         <meta http-equiv='content-type' content='text/html;charset=UTF-8'/>
         <link rel=\"stylesheet\" href=\"/css/app.css\" type \"text/css\"/>
         <title>Rum test page</title>
       </head>
       <body>
     <div id=app>"
    (rum/render-html (component))
    "</div>
     <div id=file-upload>
     </div>
     <script src='/js/app.js' type='text/javascript'></script>
       </body>
    </html>"))

(defn render-html []
  (str "<!DOCTYPE html>\n"
       (rum/render-static-markup page)))

(defn save-file! [{:keys [params]}]
  (let [{:keys [filename tempfile content-type]} (:upload-file params)]
    (unzip-file tempfile)
    (ok {:status :ok})))

(defn depersonalize [{:keys [params]}]
  (-> params
      :xml
      (update-map update-line)
      (ok)))

(defn submit [{:keys [params]}]
  (println (:xml params))
  (let [root (get-root "word_out/word/document.xml")
        updated (update-xml (:xml params) root)]
    (spit "word_out/word/document.xml" updated)
    (zip-dir "word_out" "edited.docx")
    (ok {:status :saved})))

(defroutes home-routes
  (GET "/" [] (page html/board))
  (GET "/board" [] (page html/board))
  (GET "/edited.docx" [] (file-response "edited.docx"))
  (GET "/word-xml" [] (ok (take-indexed-content "word_out/word/document.xml")))

  (POST "/upload" req (save-file! req))
  (POST "/depersonalized" req (depersonalize req))
  (POST "/submit-edit" req (submit req)))

(def app-routes
  (routes
    (wrap-home #'home-routes)
    (resources "/")
    (not-found "Not Found")))

(def app (wrap-base #'app-routes))

