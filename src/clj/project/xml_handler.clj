(ns project.xml-handler
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.zip :as z]))

(defn get-root [path]
  (z/xml-zip (xml/parse path)))

(defn update-map [m f]
  (reduce-kv (fn [m k v] (assoc m k (f v))) {} m))

(defn update-line [line]
  (mapv (fn [sym] (update sym :val #(cond-> % (:marked? sym) ((constantly "")))))
        line))

(defn go-to-tag [tag dir zip-node]
  (first
    (drop-while
      #(not (= tag (when-not (string? (z/node %))
                     (-> % (z/node) (xml/tag)))))
      (iterate dir zip-node))))

(defn get-by-tag-nested [xml-node tag]
  (filter #(= tag (:tag %))
          (xml-seq xml-node)))

(defn index-text [text]
  (map-indexed (fn [idx sym] (assoc sym :id idx)) text))

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
         (index-text)
         (group-by :wp-id))))


; Xml collect

(defn assoc-text [data wp-id wt-id]
  (->> (get data wp-id)
       (drop-while #(not (= wt-id (:wt-id %))))
       (take-while #(= wt-id (:wt-id %)))
       (map #(if (:marked? %) "" (:val %)))
       (apply str)
       (vector)))

(defn update-wt [data {:keys [loc wp-id wt-id]}]
  (-> (go-to-tag :w:t z/next loc)
      (z/edit #(assoc % :content (assoc-text data wp-id wt-id)))
      ((fn [loc] {:loc   loc
                  :wp-id wp-id
                  :wt-id (inc wt-id)}))))

(defn update-wp [data {:keys [loc wp-id]}]
  (let [wp (go-to-tag :w:p z/next loc)
        wt-count (count (get-by-tag-nested (z/node wp) :w:t))]
    (->> {:loc   wp
          :wp-id wp-id
          :wt-id 0}
         (iterate (partial update-wt data))
         (take (inc wt-count))
         (last)
         (#(update % :wp-id inc)))))

(defn update-xml [data root]
  (let [wp-count (->> root
                      (go-to-tag :w:p z/next)
                      (z/rights)
                      (count))]
    (->> {:loc root :wp-id 0}
         (iterate (partial update-wp data))
         (take (inc wp-count))
         (last)
         :loc
         (z/root)
         (xml/emit)
         (with-out-str))))

(comment
  (def docx (xml/parse "word_out/word/document.xml"))
  (def normal-docx (xml/parse "document.xml"))
  (def root (z/xml-zip docx))
  (def data (-> (take-indexed-content
                  "word_out/word/document.xml")
                (assoc-in [0 1 :marked?] true)
                (assoc-in [0 2 :marked?] true)
                (assoc-in [1 0 :val] \R)
                (assoc-in [2 0 :val] \R)))
  (def result (with-out-str (xml/emit normal-docx)))
  (spit "document_restored.xml" result)
  (def wp0 (update-wp data {:loc root :wp-id 0})))

