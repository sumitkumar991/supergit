(ns gitcloj.status
  (:require [gitcloj.constants :as cns]
            [gitcloj.helper :as hp]
            [gitcloj.reader :as rd]
            [clojure.set :as cset]
            [gitcloj.files :as fls]))

(def ^:const root cns/c-root)

(defn staged-for-commit
  "Returns a list of files staged for commit"
  [parent-dir]
  (keys (rd/get-staged parent-dir)))

(defn tracked
  "Returns a list of tracked files"
  [parent-dir]
  (keys (rd/get-curr-snapshot parent-dir)))

(defn untracked
  "Returns a list of files not being tracked by vcs"
  [parent-dir]
  (let [allfiles (apply hash-set (hp/read-relative-paths parent-dir))
        tracked (apply hash-set (tracked parent-dir))
        staged (apply hash-set (staged-for-commit parent-dir))
        ]
    (seq (cset/difference allfiles tracked staged))))

(defn modified
  "Returns a list of modified tracked files"
  [parent-dir]
  (let [files (for [i (tracked parent-dir)]
                (if (fls/changed? parent-dir i)
                  i
                  nil))
        ]
    (filter some? files)))

(defn not-staged-for-commit
  "Returns a list of modified files not staged for commit"
  [parent-dir]
  (seq
    (cset/difference (apply hash-set (modified parent-dir))
                     (apply hash-set (staged-for-commit parent-dir)))))

(defn deleted-files
  "Returns a list of deleted files"
  [parent-dir]
  (seq (cset/difference
         (apply hash-set (tracked parent-dir))
         (apply hash-set (hp/read-relative-paths parent-dir)))))
