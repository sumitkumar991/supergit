(ns gitcloj.helper
  (require [clojure.string :as cstr]
           [clojure.java.io :as io]
           [clojure.set]))
;Contains helper methods required in project
(defn read-files-recursively
  "Helper method of read-relative-paths to get absolute paths of all files from parent directory"
  ([directory]
   (let [
         allfiles (flatten
                    (let [filelist (remove #(cstr/starts-with? % ".clogit") (seq (.list (io/file directory))))]
                      (for [dir filelist]
                        (if (.isDirectory (io/file directory dir))
                          (read-files-recursively (str directory dir "/"))
                          (str directory dir)))))
         ]
     allfiles)))

(defn read-relative-paths
  "Call this method to get a list with relative paths of all the files from the given root directory"
  [parent-dir]
  (map #(cstr/replace-first % parent-dir "") (read-files-recursively parent-dir)))

(defn isChanged
  "Compares contest of 2 files from their path true if content is changed"
  [parent prev curr]
  (let [test (.contentEquals (slurp (io/file (str parent prev))) (slurp (io/file (str parent curr))))]
    test))

(defn get-untracked-files
  [parent root]
  (let [currfiles (apply hash-set (read-relative-paths parent))
        prevfiles (read-string (slurp (str parent root "index")))
        headfiles (gitcloj.reader/get-head-files parent root)
        ]
    (clojure.set/difference currfiles prevfiles headfiles)))

(defn copy-file [source-path dest-path]
  (io/copy (io/file source-path) (io/file dest-path)))
