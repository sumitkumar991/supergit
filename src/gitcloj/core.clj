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

;(defn add
;  [parent-dir fpath]
;  (let [head (rd/get-head parent-dir root) fls (get-in head [:current :files])]
;    ;(println head fpath)
;    (if (= "." fpath)
;      (let [fileset  (apply hash-set (hp/read-relative-paths parent-dir))]
;        (spit (str parent-dir headfile)
;              (update-in
;                head [:current :files] #(apply merge % fileset))))
;      (spit (str parent-dir headfile)
;            (update-in
;              head [:current :files] conj fpath)))))
(defn add-helper
  "Adds the file to index & creates the object associated"
  [parent-dir fpath]
  (let [index (rd/get-index parent-dir root)]
    (let [[dirname filename fcontent] (hp/hash-file fpath)
          dirpath (str parent-dir root "objects/" dirname "/")]
      (io/make-parents (str dirpath "nil"))
      (spit (str dirpath filename) fcontent)
      (let [stage_ob {(hp/get-relative-path parent-dir fpath) (hp/stage-file-ob 100644 filename)}]
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

(defn status
  [parent]
  (let [head (rd/get-head parent root) files (get-in head [:current :files])]
    (println "Current branch: " (get-in head [:current :name]))
    (println "Changes to be committed:" files)
    (println "Changes not staged for commit"))

  (let [untracked (hp/get-untracked-files parent root)]
    (when (not-empty untracked)
      (println "Untracked files: ")
      (loop [[i & res] (seq untracked)]
        (when i
          (println "      " i)
          (recur res))))))


(defn commit
  "copy files from head to current branch"
  [parent message]
  (let [staged (rd/get-head-files parent root)
        commitbranch (rd/get-curr-branch-path parent root)
        dirname (hp/generate-rand-str HASH_LENGTH)]

      (io/make-parents (str parent commitbranch dirname "/nil"))
      (doseq [src staged]
        (let [fname (subs src (inc (cstr/last-index-of src "/")))]
            (println fname)
          (hp/copy-file (str parent src) (str parent commitbranch dirname "/" fname))))
    ;(dosync
    ;  (io/make-parents (str parent commitbranch dirname "/nil"))
    ;  (doseq [src staged]
    ;    (let [fname (subs src (inc (cstr/last-index-of src "/")))]
    ;        (println fname)
    ;      (hp/copy-file (str parent src) (str parent commitbranch dirname "/" fname))))
    ;  ;(let [indexdata (read-string (slurp (str parent commitbranch "index")))]
    ;  ;  )
    ;  )
    ))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (add "/home/sumit/Documents/Untitled Folder/gitworks/" ".")
  (println "Hello, World!"))
