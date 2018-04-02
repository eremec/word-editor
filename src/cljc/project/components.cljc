(ns project.components
  (:require [rum.core :as rum]
            [ajax.core :as ajax :refer [GET POST]]
            [project.xml-edit :refer [slurped-xml]]))

(defn get-slurped-xml! []
  (GET "/word-xml"
       {:handler #(reset! slurped-xml
                          (vec (sort-by key %)))}))

(defn depersonalize! []
  (when-let [xml (not-empty @slurped-xml)]
    (POST "/depersonalized"
          {:params  {:xml xml}
           :handler #(reset! slurped-xml %)})))

(defn download-result []
  (GET "/download"))

(defn submit-edit []
  (POST "/submit-edit"
        {:params {:xml @slurped-xml}}))

(rum/defc sym-comp < rum/reactive [{:keys [marked? val path id]}]
  [:div.letter
   {:style {:background-color (if marked? "#00FF00" "#FFFFFF")
            :display "inline-block";
            :color (if (= val " ") "#FFFFFF" "#000000")}
    :on-click #(swap! slurped-xml
                      update
                      (first path)
                      (partial mapv (fn [l]
                                      (cond-> l
                                        (= id (:id l))
                                        (update :marked? not)))))}
   (if (= " " val) "_" val)])

(rum/defc space < rum/static []
   [:div.space
    {:style {:display "inline-block";
             :color "#FFFFFF"}}
    "______"])

(rum/defc line-comp < rum/reactive [line]
  [:div.line
   {:style {:padding "5px"
            :padding-left "20px"
            :padding-right "20px"}}
   (space)
   (for [s line]
     (rum/with-key (sym-comp s) (:id s)))])

(rum/defc block < rum/static [text f]
  [:div.block
   {:on-click f}
   [:div.small text]])

(rum/defc board < rum/reactive []
  [:div.board
   [:div.topbar
    (block "Delete highlights (not possible save changes yet)" #(depersonalize!))
;    (block "Revert"                                            #(get-slurped-xml!))
    [:div#download]]
;   (block "Submit"                                            #(submit-edit))]
;    (block "Open (if already uploaded)"                        #(get-slurped-xml!))]
   [:div.main
    (for [[wp-id line] (rum/react slurped-xml)]
      (rum/with-key (line-comp line) wp-id))]])

(def cur-page (atom :home))

(rum/defc not-found < rum/static []
  [:div [:p "not found"]])

(def pages {:home  board
            :board board})

(rum/defc page < rum/reactive []
  [:div.page
    ((if-let [p ((rum/react cur-page) pages)] p not-found))])

