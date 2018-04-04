(ns project.filesystem
  (:require [clojure.java.io :as io])
  (:import java.util.zip.ZipInputStream
           java.util.zip.ZipOutputStream
           java.util.zip.ZipEntry))

(defn clean-dir [dir])

(defn delete-recursively [fname]
  (doseq [f (reverse (file-seq (io/file fname)))]
    (when (.exists f)
      (io/delete-file f))))

;TODO Update with auto-creation directories
(defn unzip-file [file]
  (delete-recursively "word_out")
  (with-open [in (io/input-stream file)]
    (let [zs (ZipInputStream. in)]
      (loop []
        (when-let [entry (.getNextEntry zs)]
          (let [file (str "word_out/" (.getName entry))]
            (io/make-parents file)
            (io/copy zs (io/file file))
            (recur)))))))

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

