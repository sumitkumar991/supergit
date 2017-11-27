(ns gitcloj.objects
  (:require [clojure.java.io :as io]
            [gitcloj.helper :as hp]
            [clojure.string :as cstr]
            [clj-time.core :as tm]
            [clj-time.coerce :as tc]))
(def obj-dir "objects/")
; for directory
;040000 tree 0eed1217a2947f4930583229987d90fe5e8e0b74 data
; for blobs
;100664 blob 2e65efe2a145dda7ee51d1741299f848e5bf752e letter.txt
;100664 blob 56a6051ca2b02b04ef92d5150c9ef600403cb1de number.txt
; each file is terminted by a \n character
(defn create-object
  [parent-dir root uncompressed]
  (let [compressed (hp/compress uncompressed)
        [dr hashname] (hp/dir (hp/sha1-str compressed))]
    (let [path (str parent-dir root "objects/" dr "/")]
      (io/make-parents (str path "/nil"))
      (spit (str path hashname) compressed)
      (str dr hashname))))

(defn hash-dir
  [parent-dir root dirpath staged]
  (if (not (.isDirectory (io/file dirpath)))
    (let [v (get staged (hp/get-relative-path parent-dir dirpath))]
      (if (nil? v)
        nil
        (str "100644 blob " (get v :hash) " " (.getName (io/file dirpath)))))
    (let [allfiles (remove
                     #(= (.getName (io/file %)) ".clogit")
                     (seq (.listFiles (io/file dirpath)))) ]
      ;(println allfiles)
      (if (empty? allfiles)
        nil
        (let [hcontent (reduce
                         (fn [a b]
                           (str a b "\n"))
                         ""
                         (filter some? (map #(hash-dir parent-dir root % staged) allfiles)))
              hashname (hp/sha1-str hcontent)
              ]
          ;(println hcontent)
          ;(println hashname)
          (if (= hcontent "")
            nil
            (do
              (create-object parent-dir root hcontent)
              (str "040000 tree "
                   hashname
                   " " (.getName (io/file dirpath))))
            )
          )))))

(defn line-read
  "Parses a line in storage format"
  [line]
  (if (empty? line)
    nil
    (cstr/split line #"\s+")))

(defn cat-file
  "Read the content inside the object file given a file hash"
  [parent-dir root fhash]
  (try
    (let [[dr filename] (hp/dir fhash)]
      (-> (str parent-dir root obj-dir dr "/" filename)
          slurp
          hp/decompress))
    (catch Exception e
      nil)))

(defn write-file-tree
  "takes a treehash & write out files to working directory"
  [parent-dir root curr-dir treehash]
  (let [fcontent (cat-file parent-dir root treehash)
        lines (cstr/split-lines fcontent)
        parlines (remove nil? (map line-read lines))
        ]
    (let [trees (filter #(= (second %) "tree") parlines)
          blobs (filter #(= (second %) "blob") parlines)
          ]
      (doseq [tree trees]
        (println curr-dir)
        (let [cdir (str curr-dir (nth tree 3) "/nil")]
          (io/make-parents cdir)))
      (doseq [tree trees]
        (let [cdir (str curr-dir (nth tree 3) "/")]
          (write-file-tree parent-dir root cdir (nth tree 2))))
      (doseq [blob blobs]
        (spit (str curr-dir (nth blob 3)) (cat-file parent-dir root (nth blob 2)))))))

(defn make-commit-obj
  [parent-dir root parenthash treehash author committer timestamp message]
  (if (nil? parenthash)
    (let [comstr (str "tree " treehash "\n"
                      "author " author "\n"
                      "committer " committer "\n"
                      "timestamp " timestamp "\n"
                      "message " message)
          ]
      (create-object parent-dir root comstr)
      )
    (let [comstr (str "parent " parenthash "\n"
                      "tree " treehash "\n"
                      "author " author "\n"
                      "committer " committer "\n"
                      "timestamp " timestamp "\n"
                      "message " message)
          ]
      (create-object parent-dir root comstr))))

(defn make-commit
  [parent-dir root message phash snapshot]
  (let [committree (hash-dir parent-dir root parent-dir snapshot)
        treehash (nth (line-read committree) 2)
        ]
    (let [commithash (make-commit-obj
                       parent-dir root phash treehash
                       "John" "John" (tc/to-long (tm/now))
                       message)
          ]
      commithash)))
