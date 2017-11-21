(ns gitcloj.reader
  (require [clojure.java.io :as io]))

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

(defn get-head-files
  "returns the set of staged files"
  [parent-dir root]
  (get-in (get-head parent-dir root) [:current :files]))

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

