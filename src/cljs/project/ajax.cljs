(ns project.ajax
  (:require [ajax.core :as ajax]))

(defn default-headers [request]
  (-> request
      (update
        :headers
        #(merge
           %
           {"Accept"       "application/transit+json"}))))

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         into
         [(ajax/to-interceptor {:name    "default headers"
                                :request default-headers})]))

