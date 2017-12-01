(ns gitcloj.core
  (:gen-class)
  (:require [clojure.string :as cstr]
            [clojure.java.io :as io]
            [gitcloj.reader :as rd]
            [gitcloj.helper :as hp]
            [gitcloj.objects :as obj]
            [clj-time.core :as tm]
            [gitcloj.branch :as br]
            [gitcloj.constants :as cns]
            [gitcloj.status :as sts]
            [gitcloj.files :as fls]
            [gitcloj.logger :as lg]))

(def ^:const root cns/c-root)
(def ^:const APP_NAME "clogit")
(defn create-directories
  [parent-dir]
  (do
    (let [par (str parent-dir root)]
      (io/make-parents (str par "branches/nil"))
      (io/make-parents (str par "refs/nil"))
      (io/make-parents (str par "refs/heads/nil"))
      (io/make-parents (str par "logs/refs/heads/nil"))
      (io/make-parents (str par "objects/nil"))
      (spit (str par "index") "{}")                           ;index for versioned files
      (spit (str par "Head") {:ref "refs/heads/master"})
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
  (try
    (let [index (rd/get-index parent-dir)]
      (let [fcontent (slurp fpath)
            filehash (obj/create-object parent-dir fcontent)
            ]
        (let [stage_ob {(hp/get-relative-path parent-dir fpath)
                        (hp/stage-file-ob 100644 filehash)}
              ]
          (spit (str parent-dir root "index")
                (update index :staged merge stage_ob)))
        ))
    (catch Exception e
      (println "Path specified does not match any file..")))

  )

(defn add
  [parent-dir fpath]
  (let [index (rd/get-index parent-dir)]
    (cond
      (not= fpath ".") (if (.isDirectory (io/file (str parent-dir fpath)))
                         (doseq [filepath (hp/read-files-recursively
                                            (str parent-dir (if (cstr/ends-with? fpath "/")
                                                              fpath
                                                              (str fpath "/"))))
                                 ]
                           (add-helper parent-dir filepath))
                         (add-helper parent-dir (str parent-dir fpath)))
      :else (doseq [filepath (hp/read-files-recursively parent-dir)]
              (add-helper parent-dir filepath)))))

(defn reset
  "Clears the staged area files"
  [parent-dir]
  (let [indexmap (rd/get-index parent-dir)]
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
        (if (nil? branchpath)
          (spit (rd/get-index-path parent-dir) commithash)
          (spit (str parent-dir root branchpath) commithash))
        (spit (rd/get-index-path parent-dir) {:snapshot up-snap})
        ;write log entry after commit
        (lg/write-log parent-dir commithash)
        commithash))
    ))

(defn branch
  [parent-dir b]
  (if (br/branch? parent-dir b)
    (println "A branch named '" b "' already exists.")
    (do (br/create-new-branch parent-dir b)
        (println "Created new branch '" b "'"))))

(defn checkout
  "Checkout/create old/new branch or a commit"
  ([parent-dir b]
    ;(br/delete-tracked parent-dir)
   (if (br/branch? parent-dir b)
     (let [c-hash (rd/get-branch-hash parent-dir b)]
       (if (nil? c-hash)
         (println b " does not match any file(s) known to " APP_NAME)
         (let [treehash (rd/get-commit-tree parent-dir c-hash)]
           (dorun (br/delete-tracked parent-dir))
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
         (dorun (br/delete-tracked parent-dir))
         (obj/write-file-tree parent-dir parent-dir treehash)
         (spit (rd/get-index-path parent-dir)
               {:snapshot (br/build-snapshot parent-dir treehash)})
         (spit (rd/get-head-path parent-dir) b)
         (println (str "Checked out commit " b "\n" "Detached Head\n"
                       "Create new branch with [checkout -b b_name]"
                       "to save any changes in the detached state")))
       )))
  ([parent-dir op b]
   (case op
     "-b" (if (br/branch? parent-dir b)
            (println "A branch named '" b "' already exists.")
            (do
              (br/create-new-branch parent-dir b)
              (br/switch-branch parent-dir b)
              (println "Switched to a new branch '" b "'")))
     (println "Not a valid " APP_NAME))))

(defn status
  "Shows the status(untracked, staged, deleted, modified) file statuses"
  [parent-dir]
  (println "On branch " (rd/get-branch-name parent-dir))
  (let [unt (sts/untracked parent-dir)
        del (sts/deleted-files parent-dir)
        nstaged (sts/not-staged-for-commit parent-dir)
        staged (sts/staged-for-commit parent-dir)
        ]
    (if (empty? unt)
      nil
      (do (println "Untracked files: ")
          (println "  " (cstr/join "\n   " unt))))
    (if (empty? del)
      nil
      (do (println "deleted: ")
          (println "  " (cstr/join "\n   " del))))
    (if (empty? nstaged)
      nil
      (do (println "modified files not staged for commit: ")
          (println "  " (cstr/join "\n   " nstaged))))
    (if (empty? staged)
      nil
      (do (println "files staged for commit: ")
          (println "  " (cstr/join "\n   " staged))))
    (if (or unt del nstaged staged)
      nil
      "Nothing to commit, Working directory clean")))

(defn log
  "Prints out the commit log"
  [parent-dir]
  (lg/print-log parent-dir))

(defn -main
  "I don't do a whole lot ... yet."
  [& args])
