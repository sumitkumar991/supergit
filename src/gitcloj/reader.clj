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

(defn get-head-ref
  "Returns the currently referenced branch relative path"
  [parent-dir root]
  (get (get-head parent-dir root) :ref))

(defn get-ref-hash
  [parent-dir root]
  (try
    (slurp (str parent-dir root (get-head-ref parent-dir root)))
    (catch Exception e
      nil)))

(defn get-current-branch
  "Returns name of current branch"
  [parent-dir root]
  (get-in (get-head parent-dir root) [:current :name]))

(defn get-curr-branch-path
  "Returns the path from parent dir to current branch dir"
  [parent-dir root]
  (get-in (get-head parent-dir root) [:current :path]))

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
    (:snapshot data)))

(defn get-updated-snapshot
  "Returns snapshot after merging with staged files"
  [parent-dir root]
  (let [data (read-string (get-file-content (str parent-dir root "index")))]
    (merge (:snapshot data) (:staged data))))

(defn read-commit-map
  [parent-dir root commithash]
  (let [data (obj/cat-file parent-dir root commithash)
        slines (cstr/split-lines data)
        smaps (map (fn [line]
                     (let [st (cstr/split line #"\s+")]
                       (hash-map (first st) (cstr/join " " (rest st)))))
                   slines)
        ]
    (apply merge smaps)))