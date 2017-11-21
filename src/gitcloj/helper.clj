(ns gitcloj.helper
  (require [clojure.string :as cstr]
           [clojure.java.io :as io]
           [clojure.set]
           [gitcloj.reader :as rd]))
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

(defn isChanged
  "Compares contest of 2 files from their path true if content is changed"
  [parent prev curr]
  (let [test (.contentEquals (slurp (io/file (str parent prev))) (slurp (io/file (str parent curr))))]
    test))

(defn get-untracked-files
  [parent root]
  (let [currfiles (apply hash-set (read-relative-paths parent))
        prevfiles (read-string (slurp (str parent root "index")))
        headfiles (gitcloj.reader/get-head-files parent root)]

    (clojure.set/difference currfiles prevfiles headfiles)))

(defn copy-file [source-path dest-path]
  (io/copy (io/file source-path) (io/file dest-path)))

(defn generate-rand-str
  [length]
  (let [randstr (apply str
                       (map #(char %)
                            (repeatedly length
                                        #(+ 97 (rand-int 26)))))]

    randstr))

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
  content)

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

(defn blob-object
  [perm fhash name]
  {})

(defn commit-object
  [childhash author committer comment]
  {
   :tree childhash
   :author author
   :committer committer
   :comment comment})

(defn generate-snapshot
  "Merges the latest snapshot with staged files & returns their sha1 hash"
  [parent-dir root]
  (let [index (rd/get-index parent-dir root)]
    (-> (merge (:snapshot index) (:staged index))
        str
        sha1-str)))