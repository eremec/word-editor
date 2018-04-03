(ns project.scratch
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.zip :as z]))

(defn go-to-tag [tag dir zip-node]
  (first
    (drop-while
      #(not (= tag (when-not (string? (z/node %))
                     (-> % (z/node) (xml/tag)))))
      (iterate dir zip-node))))

(defn get-by-tag-nested [xml-node tag]
  (filter #(= tag (:tag %))
          (xml-seq xml-node)))

(defn take-text [wt-idx {:keys [wp-idx wt]}]
  (if-let [content (:content wt)]
    (mapv (fn [sym] {:wp-id wp-idx
                     :wt-id wt-idx
                     :val   sym
                     :marked? false})
         (first content))
    (vector
      {:wp-id wp-idx
       :wt-id wt-idx
       :val   nil
       :marked? false})))

(defn take-indexed-wt [wp-idx wp]
  (let [wts (get-by-tag-nested wp :w:t)]
    (apply concat
           (map-indexed #(take-text %1 {:wp-idx wp-idx :wt %2})
                        wts))))

(defn take-indexed-content [doc-path]
  (let [root (z/xml-zip (xml/parse doc-path))
        left-wp (go-to-tag :w:p z/next root)]
    (->> (apply
           concat
           (map-indexed take-indexed-wt
                        (-> (z/rights left-wp)
                            (conj (z/node left-wp))
                            (drop-last))))
         (group-by :wp-id))))

(def data (atom (take-indexed-content
                  "word_out/word/document.xml")))

(def root (z/xml-zip (xml/parse "word_out/word/document.xml"))
(def left-wp (go-to-tag :w:p z/next root
(def wps-seq (-> (z/rights left-wp)
                 (conj (z/node left-wp))
                 (drop-last)))
(def wts0 (get-by-tag-nested (first wps-seq) :w:t))
(def wts8 (get-by-tag-nested (last (take 9 wps-seq)) :w:t))
(defn get-from-indexed [content wp-id wt-id]
  (->> (get content wp-id)
       (drop-while #(not (= wt-id (:wt-id %))))
       (take-while #(= wt-id (:wt-id %)))
       (map :val)))
