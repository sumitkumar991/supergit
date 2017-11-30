(ns gitcloj.reader
  (require [clojure.java.io :as io]
           [gitcloj.objects :as obj]
           [clojure.string :as cstr]
           [gitcloj.constants :as cns]))

(def ^:const root cns/c-root)
(defn get-file-content
  "Tries to read a file"
  [path]
  (try
    (slurp path)
    (catch Exception e
      (println "No such file exists"))))

(defn get-head
  [parent]
  (let [data (get-file-content (str parent root "Head"))]
    (try
      (read-string data)
      (catch Exception e
        data))))

(defn get-head-path
  [parent-dir]
  (str parent-dir root "Head"))

(defn get-head-ref
  "Returns the currently referenced branch relative path"
  [parent-dir]
  (let [strr (get-head parent-dir)]
    (if (string? strr)
      nil
      (get (get-head parent-dir) :ref))))

(defn get-head-hash
  [parent-dir]
  (try
    (if (nil? (get-head-ref parent-dir))
      (get-head parent-dir)
      (slurp (str parent-dir root (get-head-ref parent-dir))))
    (catch Exception e
      nil)))

(defn get-branch-hash
  "Returns the hash pointed by the ref of branch"
  [parent-dir bname]
  (try
    (slurp (str parent-dir root "refs/heads/" bname))
    (catch Exception e
      nil)))

(defn get-index
  "Returns the content of index file"
  [parent-dir]
  (read-string (get-file-content (str parent-dir root "index"))))

(defn get-curr-snapshot
  "Returns current state of working directory"
  [parent-dir]
  (let [data (read-string (get-file-content (str parent-dir root "index")))]
    (:snapshot data)))

(defn get-staged
  "Returns files staged for commit"
  [parent-dir]
  (let [data (read-string (get-file-content (str parent-dir root "index")))]
    (:staged data)))

(defn get-updated-snapshot
  "Returns snapshot after merging with staged files"
  [parent-dir]
  (let [data (read-string (get-file-content (str parent-dir root "index")))]
    (merge (:snapshot data) (:staged data))))

(defn read-commit-map
  [parent-dir commithash]
  (let [data (obj/cat-file parent-dir commithash)]
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
  [parent-dir commithash]
  (try
    (get (read-commit-map parent-dir commithash) "tree")
    (catch Exception e
      nil)))

(defn get-index-path
  [parent-dir]
  (str parent-dir root "index"))

(defn indexed-hash
  "Returns the hash of indexed file from snapshot"
  [parent-dir rpath]
  (get-in (get-curr-snapshot parent-dir) [rpath :hash]))

(defn get-branch-name
  [parent-dir]
  (let [head (get-head parent-dir)]
    (if (coll? head)
      (let [bpath (:ref head)]
        (subs bpath (inc (cstr/last-index-of bpath "/"))))
      head)))
