(ns gitcloj.files
  (:gen-class)
  (:require [clojure.java.io :as io]
            [gitcloj.helper :as hp]
            [gitcloj.constants :as cns]
            [gitcloj.reader :as rd]))
(def ^:const root cns/c-root)

(defn changed?
  "Returns true if a file has been changed given it's path relative
  from root dir path"
  [parent-dir fpath]
  (let [fhash (rd/indexed-hash parent-dir fpath)]
    (if (nil? fhash)
      nil
      (=
        (hp/sha1-str (hp/compress (slurp (str parent-dir fpath))))
        fhash))))

(defn indexed?
  "Returns true if the given file path is indexed"
  [parent-dir fpath]
  (let [index (rd/get-index parent-dir)]
    (if (or (get-in index [:staged fpath]) (get-in index [:snapshot fpath]))
      true
      false)))

(defn remove-from-index
  "Removes a file entry from the index snapshot & staged"
  [parent-dir filep]
  (let [data (rd/get-index parent-dir)]
    (let [rsnap (dissoc (:snapshot data) filep)
          rstage (dissoc (:staged data) filep)
          ]
      (spit (str parent-dir root "index") {:snapshot rsnap :staged rstage}))))

(defn remove-file
  "Remove file from index & working directory"
  [parent-dir fpath]
  (try
    (if (indexed? parent-dir fpath)
      (if (io/delete-file (str parent-dir fpath))
          (do (println "'" fpath "'" "deleted from working copy")
              (remove-from-index parent-dir fpath)
              (println "file removed from index"))
          (println fpath " file remove failed"))
      (println "'" fpath "' is not under version control")
      )
    (catch Exception e
      nil)))
