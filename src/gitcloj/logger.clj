(ns gitcloj.logger
  (:require [gitcloj.reader :as rd]
            [gitcloj.constants :as cns]))

(def ^:const root cns/c-root)

(defn- commit-log
  [parent-dir commithash]
  (let [com (rd/read-commit-map parent-dir commithash)
        parent (get com "parent")]
    (str
      (if parent (reduce str (repeat 40 \0)) parent) " "
      commithash " "
      (get com "committer") " "
      (get com "timestamp") " "
      (get com "message" "\n"))))

(defn write-log
  [parent-dir commithash]
  (let [l (commit-log parent-dir commithash)]
    (spit (str parent-dir root "/logs/HEAD") l :append true)))
