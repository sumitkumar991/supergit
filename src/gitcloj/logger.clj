(ns gitcloj.logger
  (:require [gitcloj.reader :as rd]
            [gitcloj.constants :as cns]
            [clojure.string :as cstr]))

(def ^:const root cns/c-root)

(defn- commit-log
  [parent-dir commithash]
  (let [com (rd/read-commit-map parent-dir commithash)
        parent (get com "parent")]
    (str
      (if parent (reduce str (repeat 40 \0)) parent) " "
      commithash " "
      "Author: " (get com "author") " "
      "Date: " (get com "timestamp") " "
      "commit: " (get com "message" "\n"))))

(defn write-log
  [parent-dir commithash]
  (let [l (commit-log parent-dir commithash)
        br (rd/get-branch-name parent-dir)]
    (spit (str parent-dir root "/logs/refs/heads/" br) l :append true)))

(defn- line-splitter
  "Takes a line of commit log & splits it accordingly"
  [line]
  (let [sp (cstr/split line #"\s+")
        parent (nth sp 0)
        latest (nth sp 1)
        committer (nth sp 2)
        tstamp (nth sp 3)
        msg (reduce str (drop 4 sp))
        ]
    (list parent latest committer tstamp msg)))

(defn print-log
  "Pritnts out commit log on current branch"
  [parent-dir]
  (let [ss (slurp (str parent-dir root "logs/refs/heads/" (rd/get-branch-name parent-dir)))
        lines (map
                #(list (nth % 1) (nth % 2) (nth % 3) (nth % 4))
                (map line-splitter (cstr/split ss #"\n")))
        pdata (map #(cstr/join % "\n"))
        ]
    ;(println (cstr/join (first lines) "\n"))
    (doseq [s lines]
      (println (cstr/join "\n" s)))))
