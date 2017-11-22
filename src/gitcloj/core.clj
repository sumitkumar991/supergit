(ns gitcloj.core
  (:gen-class)
  (require [clojure.string :as cstr]
           [clojure.java.io :as io]
           [gitcloj.reader :as rd]
           [gitcloj.helper :as hp]))

(def ^:const root ".clogit/")
(def ^:const headfile (str root "Head"))
(def ^:const HASH_LENGTH 8)
(defn create-directories
  [parent]
  (do
    (let [par (str parent root)]
      (io/make-parents (str par "branches/nil"))
      (io/make-parents (str par "refs/nil"))
      (io/make-parents (str par "refs/heads/nil"))
      (io/make-parents (str par "branches/master/nil"))
      (io/make-parents (str par "logs/master"))
      (io/make-parents (str par "objects/nil"))
      (spit (str par "index") "{}")                           ;index for versioned files
      (spit (str par "Head") {:ref "refs/heads/master"})

      (spit (str par "branches/Index")
            {"master" {}})
      (spit (str par "branches/master/index") "{}"))))


(defn init
  "Checks if current directory is under version control & creates new if not"
  [parent]
  (if (.isDirectory (io/file (str parent root)))
    (println "The Directory is already under vcs")
    (create-directories parent)))

(defn add-helper
  "Adds the file to index & creates the object associated"
  [parent-dir fpath]
  (let [index (rd/get-index parent-dir root)]
    (let [[dirname filename fcontent] (hp/hash-file fpath)
          dirpath (str parent-dir root "objects/" dirname "/")]
      (io/make-parents (str dirpath "nil"))
      (spit (str dirpath filename) fcontent)
      (let [stage_ob {(hp/get-relative-path parent-dir fpath)
                      (hp/stage-file-ob 100644 (str dirname filename))}
            ]
        (spit (str parent-dir root "index")
              (update index :staged merge stage_ob)))
      )))

(defn add
  [parent-dir fpath]
  (let [index (rd/get-index parent-dir root)]
    (cond
      (not= fpath ".") (if (.isDirectory (io/file (str parent-dir fpath)))
                         (doseq [filepath (hp/read-files-recursively (str parent-dir fpath))]
                           (add-helper parent-dir filepath))
                         (add-helper parent-dir (str parent-dir fpath)))
      :else (doseq [filepath (hp/read-files-recursively parent-dir)]
              (add-helper parent-dir filepath)))))

(defn commit
  "Commit the updated snapshot & save hash at current branch"
  [parent-dir comment]
  (let [snaphash (hp/save-snapshot parent-dir root)]
    (let [committree (hp/commit-tree-object snaphash "John" "Doe" comment)
          treehash (hp/create-object parent-dir root committree)]
      ;treehash is the hash of actual commit
      (spit
        (str parent-dir root (rd/get-head-ref parent-dir root))
        treehash)
      treehash)))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (add "/home/sumit/Documents/Untitled Folder/gitworks/" ".")
  (println (commit "/home/sumit/Documents/Untitled Folder/gitworks/" "jlfnal"))
  (println "Hello, World!"))
