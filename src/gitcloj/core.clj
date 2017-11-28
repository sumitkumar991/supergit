(ns gitcloj.core
  (:gen-class)
  (:require [clojure.string :as cstr]
            [clojure.java.io :as io]
            [gitcloj.reader :as rd]
            [gitcloj.helper :as hp]
            [gitcloj.objects :as obj]
            [clj-time.core :as tm]
            [gitcloj.branch :as br]
            [gitcloj.constants :as cns]))

(def ^:const root cns/c-root)
(def ^:const APP_NAME "clogit")
(defn create-directories
  [parent-dir]
  (do
    (let [par (str parent-dir root)]
      (io/make-parents (str par "branches/nil"))
      (io/make-parents (str par "refs/nil"))
      (io/make-parents (str par "refs/heads/nil"))
      ;(io/make-parents (str par "branches/master/nil"))
      (io/make-parents (str par "logs/master"))
      (io/make-parents (str par "objects/nil"))
      (spit (str par "index") "{}")                           ;index for versioned files
      (spit (str par "Head") {:ref "refs/heads/master"})

      ;(spit (str par "branches/Index")
      ;      {"master" {}})
      ;(spit (str par "branches/master/index") "{}")
      )))


(defn init
  "Checks if current directory is under version control & creates new if not"
  [parent-dir]
  (if (.isDirectory (io/file (str parent-dir root)))
    (println "The Directory is already under vcs")
    (do (create-directories parent-dir)
        (println "Initialized empty Supergit repository"))))

(defn add-helper
  "Adds the file to index & creates the object associated"
  [parent-dir fpath]
  (let [index (rd/get-index parent-dir)]
    (let [fcontent (slurp fpath)
          filehash (obj/create-object parent-dir fcontent)
          ]
      (let [stage_ob {(hp/get-relative-path parent-dir fpath)
                      (hp/stage-file-ob 100644 filehash)}
            ]
        (spit (str parent-dir root "index")
              (update index :staged merge stage_ob)))
      )))

(defn add
  [parent-dir fpath]
  (let [index (rd/get-index parent-dir)]
    (cond
      (not= fpath ".") (if (.isDirectory (io/file (str parent-dir fpath)))
                         (doseq [filepath (hp/read-files-recursively (str parent-dir fpath))]
                           (add-helper parent-dir filepath))
                         (add-helper parent-dir (str parent-dir fpath)))
      :else (doseq [filepath (hp/read-files-recursively parent-dir)]
              (add-helper parent-dir filepath)))))

(defn reset
  [parent-dir root]
  (let [indexmap (rd/get-index parent-dir)]
    (println indexmap)
    (spit
      (rd/get-index-path parent-dir) (update indexmap :staged (fn [x] nil)))))

(defn commit
  "Commit the updated snapshot & save hash at current branch"
  [parent-dir message]
  (let [parenthash (rd/get-head-hash parent-dir)
        staged (rd/get-staged parent-dir)]
    (if (or (nil? staged) (= {} staged))
      (println "nothing added to commit")
      (let [up-snap (rd/get-updated-snapshot parent-dir)
            commithash (obj/make-commit parent-dir message parenthash up-snap)
            branchpath (rd/get-head-ref parent-dir)
            ]
        (spit (str parent-dir root branchpath) commithash)
        (spit (rd/get-index-path parent-dir) {:snapshot up-snap})
        commithash))
    ))

(defn branch
  [parent-dir b]
  (if (br/branch? parent-dir b)
    (println "A branch named '" b "' already exists.")
    (do (br/create-new-branch parent-dir b)
        (println "Created new branch '" b "'"))))

;todo: delete tracked files on branch checkout
(defn checkout
  "Checkout/create old/new branch or a commit"
  ([parent-dir b]
   ;(br/delete-tracked parent-dir)
   (if (br/branch? parent-dir b)
     (let [c-hash (rd/get-branch-hash parent-dir b)]
       (if (nil? c-hash)
         (println b " does not match any file(s) known to " APP_NAME)
         (let [treehash (rd/get-commit-tree parent-dir c-hash)]
           (br/delete-tracked parent-dir)
           (obj/write-file-tree parent-dir parent-dir treehash)
           (spit (rd/get-index-path parent-dir)
                 {:snapshot (br/build-snapshot parent-dir treehash)})
           (br/switch-branch parent-dir b)
           (println "Switched to branch " b)))
       )
     (let [treehash (rd/get-commit-tree parent-dir b)]
       (if (nil? treehash)
         (println b " does not match any file(s) known to " APP_NAME))
       (do
         (br/delete-tracked parent-dir)
         (obj/write-file-tree parent-dir parent-dir treehash)
         (spit (rd/get-index-path parent-dir)
               {:snapshot (br/build-snapshot parent-dir root treehash)})
         (spit (rd/get-head-path parent-dir) b)
         (println (str "Checked out commit " b "\n" "Detached Head\n"
                       "Create new branch with [checkout -b b_name]"
                       "to save any changes in the detached state")))
       )))
  ([parent-dir op b]
    (case op
      "-b" (if (br/branch? parent-dir b)
             (println "A branch named '" b "' already exists.")
             (do (br/create-new-branch parent-dir b)
                 (br/switch-branch parent-dir b)
                 (println "Switched to a new branch '" b "'")))
      (println "Not a valid " APP_NAME))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;(init "/home/sumit/Documents/Untitled Folder/demo/")
  ;(add "/home/sumit/Documents/Untitled Folder/demo/" "folder1/")
  ;(add "/home/sumit/Documents/Untitled Folder/demo/" "check/")
  ;(add "/home/sumit/Documents/Untitled Folder/demo/" "file1")
  ;(reset "/home/sumit/Documents/Untitled Folder/demo/")
  ;(println (commit "/home/sumit/Documents/Untitled Folder/demo/" "checkout-test"))
  ;(println (obj/hash-dir "/home/sumit/Documents/Untitled Folder/demo/"
  ;                       "/home/sumit/Documents/Untitled Folder/demo/"
  ;                       (rd/get-updated-snapshot "/home/sumit/Documents/Untitled Folder/demo/" root)))
  ;
  ;(obj/write-file-tree "/home/sumit/Documents/Untitled Folder/demo/" root "/home/sumit/Documents/Untitled Folder/demo/" "05172486ce931e6f36f9138786e3f9bd20b900f9")
  (checkout "/home/sumit/Documents/Untitled Folder/demo/" "master")
  (println "Hello, World!"))
