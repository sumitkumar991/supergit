(ns gitcloj.reader
  (require [clojure.java.io :as io]
           [gitcloj.objects :as obj]
           [clojure.string :as cstr]))

(defn get-file-content
  "Tries to read a file"
  [path]
  (try
    (slurp path)
    (catch Exception e
      (println "No such file exists"))))

(defn get-head
  [parent root]
  (read-string (get-file-content (str parent root "Head"))))

(defn get-head-path
  [parent-dir root]
  (str parent-dir root "Head"))

(defn get-head-ref
  "Returns the currently referenced branch relative path"
  [parent-dir root]
  (let [strr (get-head parent-dir root)]
    (if (string? strr)
      nil
      (get (get-head parent-dir root) :ref))))

(defn get-head-hash
  [parent-dir root]
  (try
    (if (nil? (get-head-ref parent-dir root))
      (get-head parent-dir root)
      (slurp (str parent-dir root (get-head-ref parent-dir root))))
    (catch Exception e
      nil)))

(defn get-branch-hash
  "Returns the hash pointed by the ref of branch"
  [parent-dir root bname]
  (try
    (slurp (str parent-dir root "refs/heads/" bname))
    (catch Exception e
      nil)))

(defn get-index
  "Returns the content of index file"
  [parent-dir root]
  (read-string (get-file-content (str parent-dir root "index"))))

(defn get-curr-snapshot
  "Returns current state of working directory"
  [parent-dir root]
  (let [data (read-string (get-file-content (str parent-dir root "index")))]
    (:snapshot data)))

(defn get-staged
  "Returns files staged for commit"
  [parent-dir root]
  (let [data (read-string (get-file-content (str parent-dir root "index")))]
    (:staged data)))

(defn get-updated-snapshot
  "Returns snapshot after merging with staged files"
  [parent-dir root]
  (let [data (read-string (get-file-content (str parent-dir root "index")))]
    (merge (:snapshot data) (:staged data))))

(defn read-commit-map
  [parent-dir root commithash]
  (let [data (obj/cat-file parent-dir root commithash)]
    (if (nil? data)
      nil
      (let [slines (cstr/split-lines data)
            smaps (map (fn [line]
                         (let [st (cstr/split line #"\s+")]
                           (hash-map (first st) (cstr/join " " (rest st)))))
                       slines)
            ]
        (apply merge smaps)))))

(defn get-commit-tree
  [parent-dir root commithash]
  (get (read-commit-map parent-dir root commithash) "tree"))

(defn get-index-path
  [parent-dir root]
  (str parent-dir root "index"))
