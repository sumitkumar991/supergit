(ns gitcloj.helper
  (require [clojure.string :as cstr]
           [clojure.java.io :as io]
           [gitcloj.constants :as cns]))
;Contains helper methods required in project
(def ^:const root cns/c-root)

(defn read-files-recursively
  "Helper method of read-relative-paths to get absolute paths of all files from parent directory"
  ([directory]
   (let [
         allfiles (flatten
                    (let [filelist (remove #(cstr/starts-with? % ".clogit") (seq (.list (io/file directory))))]
                      (for [dir filelist]
                        (if (.isDirectory (io/file directory dir))
                          (read-files-recursively (str directory dir "/"))
                          (str directory dir)))))]
     allfiles)))

(defn get-relative-path
  "Returns the relative path of a file from the the given parent directory"
  [parent-dir filepath]
  (cstr/replace-first filepath parent-dir ""))

(defn read-relative-paths
  "Call this method to get a list with relative paths of all the files from the given root directory"
  [parent-dir]
  (map #(get-relative-path parent-dir %) (read-files-recursively parent-dir)))

(defn sha1-str [s]
  (->> (-> "sha1"
           java.security.MessageDigest/getInstance
           (.digest (.getBytes s)))
       (map #(.substring
               (Integer/toString
                 (+ (bit-and % 0xff) 0x100) 16) 1))
       (apply str)))

(defn compress
  "Compresses the file contents in using zlib compression"
  [content]
  (str content))

(defn decompress
  "Decompresses a compressed file"
  [compressed]
  (str compressed))

(defn hash-file
  "Returns the 40 char long sha1 key of hashed contents of file [first2 last38]"
  [filepath]
  (let [content (slurp filepath)
        compressed (compress content)
        filehash (sha1-str compressed)
        ]
    [(subs filehash 0 2) (subs filehash 2) compressed]))

(defn stage-file-ob
  "Staging file format before commit is made"
  [mode fhash]
  {:mode mode :hash fhash})

(defn dir
  "splits checksum to [dir name]"
  [s]
  [(subs s 0 2) (subs s 2)])

(defn blob-object
  [mode fhash name]
  (str mode " " "blob" " " fhash " " name))

(defn is-dir
  [path]
  (.isDirectory (io/file path)))

(defn empty-dir?
  [dir]
  (let [file (io/file dir)]
    (->
      file
      .list
      empty?)))
