(ns project.xml-handler
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.zip :as z]))

(defn update-map [m f]
  (reduce-kv (fn [m k v] (assoc m k (f v))) {} m))

(defn update-line [line]
  (mapv (fn [sym] (update sym :val #(cond-> % (:marked? sym) ((constantly "")))))
        line))

(defn take-text [wr-idx {:keys [wp-idx wr]}]
  (->> (xml-seq wr)
       (filter #(= :w:t (xml/tag %)))
       (first)
       :content
       (first)
       (map (fn [sym] {:path [wp-idx wr-idx]
                       :val  sym
                       :marked? false}))))

(defn take-indexed-wr [wp-idx wp]
  (let [pPr (-> wp (z/xml-zip) (z/down))
        wrs (if (= (xml/tag (z/node pPr)) :w:r)
                (conj (z/rights pPr) (z/node pPr))
                (z/rights pPr))]
    (apply concat
           (map-indexed #(take-text %1 {:wp-idx wp-idx :wr %2})
                        wrs))))

(defn index-text [text]
  (map-indexed (fn [idx sym] (assoc sym :id idx)) text))

(defn group-indexed-text [text]
  (group-by #(-> % :path first) text))

(defn take-indexed-content [doc-path]
  (let [root (z/xml-zip (xml/parse doc-path))
        left-wp (-> root (z/down) (z/down))]
    (-> (apply
          concat
          (map-indexed take-indexed-wr
                       (conj (z/rights left-wp)
                             (z/node left-wp))))
        (index-text)
        (group-indexed-text))))

