(ns project.filesystem
  (:require [clojure.java.io :as io])
  (:import java.util.zip.ZipInputStream
           java.util.zip.ZipOutputStream
           java.util.zip.ZipEntry))

(defn clean-dir [dir])

;TODO Update with auto-creation directories
(defn unzip-file [file]
  (let [in (io/input-stream (.getPath file))
        zs (ZipInputStream. in)]
    (loop []
      (when-let [next-entry (.getNextEntry zs)]
        (io/copy zs (io/file (str "word_out/" (.getName next-entry))))
        (recur)))))

(defn get-dir-filenames [dir]
  (->> dir
       (io/file)
       (file-seq)
       (filter #(.isFile %))
       (map #(.getPath %))))

;TODO Wrap with macros
(defn zip-dir [dir out]
  (let [zip (ZipOutputStream. (io/output-stream out))
        l (inc (count dir))]
    (doseq [filename (get-dir-filenames dir)]
      (.putNextEntry zip (ZipEntry. (subs filename l)))
      (io/copy (io/input-stream filename) zip)
      (.closeEntry zip))
    (.close zip)))

