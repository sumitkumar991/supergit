(ns gitcloj.helper
  (require [clojure.string :as cstr]
           [clojure.java.io :as io]
           [clojure.set]
           [gitcloj.reader :as rd]
           [clojure.pprint]))
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
  [parent root])
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

(defn gen-snapshot-checksum
  "Merges the latest snapshot with staged files & returns their sha1 hash"
  [parent-dir root]
  (let [index (rd/get-index parent-dir root)]
    (let [checksum (-> (rd/get-updated-snapshot parent-dir root)
                       str
                       sha1-str)
          ]
      [(subs checksum 0 2) (subs checksum 2)])))

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

;snapshot save in format
; for directory
;040000 tree 0eed1217a2947f4930583229987d90fe5e8e0b74 data
; for blobs
;100664 blob 2e65efe2a145dda7ee51d1741299f848e5bf752e letter.txt
;100664 blob 56a6051ca2b02b04ef92d5150c9ef600403cb1de number.txt

(defn save-snapshot
  "Saves the current state of working copy & returns the hashname of saved file"
  [parent-dir root]
  ;(let [snapmap (rd/get-updated-snapshot parent-dir root)]
  ;  ;(clojure.pprint/pprint snapmap)
  ;  (let [kys (keys snapmap)]
  ;    )
  ;  ;(create-object parent-dir root snapmap)
  ;  )
  )

(defn commit-tree-object
  [parenthash treehash author committer comment]
  {
   :tree treehash
   :author author
   :committer committer
   :comment comment})

;use when dirname & hash are required
(def drcompress (comp dir sha1-str compress))