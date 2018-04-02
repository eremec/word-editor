(ns project.middleware
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.reload :refer [wrap-reload]]))

(defn wrap-nocache [handler]
  (fn [request]
    (-> request
        handler
        (assoc-in [:headers "Pragma"] "no-cache"))))

(defn wrap-base [handler]
  (-> handler
      (wrap-defaults (assoc-in
                       (assoc-in site-defaults [:security :anti-forgery] false)
                       [:params :keywordize]
                       true))
      (wrap-nocache)))

(defn wrap-home [handler]
  (-> handler
      (wrap-restful-format)
      (wrap-multipart-params)))
