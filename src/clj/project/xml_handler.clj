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

(comment
  (def docx (xml/parse "word_out/word/document.xml"))
  (def root (z/xml-zip docx))
  (def data (atom (take-indexed-content
                    "word_out/word/document.xml")))

  (defn get-from-indexed [content wp-id wr-id]
    (->> (get content wp-id)
         (drop-while #(not (= wr-id (-> % :path second))))
         (take-while #(= wr-id (-> % :path second)))
         (map :val)))


  (defn go-to-tag [tag dir zip-node]
    (first
      (drop-while
        #(not (= tag (when-not (string? (z/node %))
                               (-> % (z/node) (xml/tag)))))
        (iterate dir zip-node))))

  (defn tag-contains? [loc tag]
    (not-empty (filter #(partial = tag (:tag %))
                       (-> loc (z/node) (xml-seq)))))

  (defn update-text [_ wp-id wr-id]
    (when _
      (->> (get @data wp-id)
           (drop-while #(not (= wr-id (-> % :path second))))
           (take-while #(= wr-id (-> % :path second)))
           (map :val)
           (apply str)
           (vector))))

  (defn update-wr-and-go-next [{:keys [loc wp-id wr-id]}]
    (println "wr: " wr-id " wp: " wp-id)
    (let [wr
          (if (tag-contains? loc :w:t)
            (go-to-tag :w:r z/prev
              (z/edit (go-to-tag :w:t z/next loc)
                      #(update % :content update-text wp-id wr-id)))
            loc)]
      (if-let [r (z/right wr)]
         {:loc r :wp-id wp-id :wr-id (inc wr-id)}
         {:ended? true
          :loc (go-to-tag :w:p z/next wr)
          :wp-id wp-id
          :wr-id (inc wr-id)})))

  (defn update-wp-and-go-next [{:keys [loc wp-id]}]
    (println wp-id)
    (->> loc
         (go-to-tag :w:r z/next)
         ((fn [new-loc] {:loc new-loc :wp-id  wp-id :wr-id 0}))
         (iterate update-wr-and-go-next)
         (take-while #(not (:ended? %)))
         (last)
         (update-wr-and-go-next)
         ((fn [new-data] (update new-data :wp-id inc)))))

  (defn update-xml [root]
    (->> root
         (go-to-tag :w:p z/next)
         ((fn [loc] {:loc loc :wp-id 0}))
         (iterate update-wp-and-go-next)
  ;       (take-while (fn [{:keys [loc]}] (z/right loc)))
         (take-while (fn [{:keys [loc]}] (= :w:p (xml/tag (z/node loc)))))
         (last)
         :loc
         (z/root)))

  (def wp (go-to-tag :w:p z/next root))
  (def wr0 (go-to-tag :w:r z/next root))
  (def edited-wr0 (update-wr-and-go-next {:loc wr0 :wp-id 0 :wr-id 0}))
  (def edited-wr1 (update-wr-and-go-next
                    {:loc (go-to-tag :w:r z/next (:loc edited-wr0))
                     :wp-id 1
                     :wr-id 0}))
  (def edited-wr2 (update-wr-and-go-next
                    {:loc (go-to-tag :w:r z/next (:loc edited-wr1))
                     :wp-id 2
                     :wr-id 0}))

  (def wp0 {:loc wp :wp-id 0 :wr-id 0})
  (def edited0 (update-wp-and-go-next {:loc root :wp-id 0}))
  (def edited1 (update-wp-and-go-next edited0))
  (def edited2 (update-wp-and-go-next edited1))
  (def edited3 (update-wp-and-go-next edited2))
  (def edited4 (update-wp-and-go-next edited3))
  (def edited5 (update-wp-and-go-next edited4))
  (def edited6 (update-wp-and-go-next edited5))
  (def edited7 (update-wp-and-go-next edited6))
  (def edited8 (update-wp-and-go-next edited7))
  (def edited9 (update-wp-and-go-next edited8))

  (def edited-wps (filter #(= :w:p (:tag %)) (xml-seq (z/root (:loc (last taked))))))
  (def iter (iterate update-wp-and-go-next {:loc root :wp-id 0}))
  (def taked (take 6 iter))

  (def new-root (z/root (:loc edited0)))
  ;(def new-root (update-xml root))
  (map :val (get (take-indexed-content (z/xml-zip new-root)) 0))
  (map :val (get (take-indexed-content (z/xml-zip new-root)) 1))
  (map :val (get (take-indexed-content (z/xml-zip new-root)) 2))
  (map :val (get (take-indexed-content (z/xml-zip new-root)) 3))
  (map :val (get (take-indexed-content (z/xml-zip new-root)) 4))
  (map :val (get (take-indexed-content (z/xml-zip new-root)) 5))
  (map :val (get (take-indexed-content (z/xml-zip new-root)) 6)))

