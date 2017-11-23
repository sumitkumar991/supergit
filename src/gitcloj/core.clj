(ns gitcloj.core
  (:gen-class)
  (require [clojure.string :as cstr]
           [clojure.java.io :as io]
           [gitcloj.reader :as rd]
           [gitcloj.helper :as hp]
           [gitcloj.objects :as obj]
           [clj-time.core :as tm]))

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
  [parent-dir message]
  (let [parenthash (rd/get-ref-hash parent-dir root)]
    (let [commithash (obj/make-commit parent-dir root message parenthash)
          branchpath (rd/get-head-ref parent-dir root)
          ]
      (spit (str parent-dir root branchpath) commithash)
      commithash)))
  ;(let [snaphash (hp/save-snapshot parent-dir root)]
  ;  (let [committree (hp/commit-tree-object snaphash "John" "Doe" comment)
  ;        treehash (obj/create-object parent-dir root committree)]
  ;    ;treehash is the hash of actual commit
  ;    (spit
  ;      (str parent-dir root (rd/get-head-ref parent-dir root))
  ;      treehash)
  ;    treehash))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;(add "/home/sumit/Documents/Untitled Folder/path/to/" ".")
  (println (commit "/home/sumit/Documents/Untitled Folder/path/to/" "phew finally"))
  ;(obj/hash-dir "/home/sumit/Documents/Untitled Folder/path/to/" root "/home/sumit/Documents/Untitled Folder/path/to")
  ;(obj/write-file-tree "/home/sumit/Documents/Untitled Folder/path/to/" root "/home/sumit/Documents/Untitled Folder/path/to/" "e77bc84dfb2f49f0027b88f2ed4a710813b70dc0")

  (println "Hello, World!"))
