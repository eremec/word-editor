(ns project.server
  (:require [project.handler :refer [app]]
            [org.httpkit.server :as http-kit]))

(defonce server (atom nil))

(defn stop-server! []
  (when-let [stop-fn @server]
    (stop-fn)))

(defn -main [& args]
  (reset! server (http-kit/run-server #'app {:port 3000})))

(comment
  (reset! server (http-kit/run-server #'app {:port 3000}))
  (stop-server!)
  (inc 1))

