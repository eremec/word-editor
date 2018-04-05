(ns project.core
  (:require [accountant.core :as accountant]
            [reagent.core :as r]
            [bidi.bidi :as bidi]
            [project.ajax :refer [load-interceptors!]]
            [project.components :as c]
            [project.components-js :refer [upload-comp-for-mount]]))


(def routes ["/" {""                :home
                  "about"           :about
                  "board"           :board}])

(defn ^:export init! []
  (load-interceptors!)
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (let [{current-page :handler
              route-params :route-params}
             (bidi/match-route routes path)]
         (reset! c/cur-page current-page)))
     :path-exists?
     (fn [path]
       (boolean (bidi/match-route routes path)))})
  (accountant/dispatch-current!)
  (r/render [upload-comp-for-mount]
            (js/document.getElementById "app")))



