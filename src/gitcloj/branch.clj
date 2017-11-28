(ns gitcloj.branch
  (:require [clojure.java.io :as io]
            [clojure.string :as cstr]
            [gitcloj.objects :as obj]
            [gitcloj.reader :as rd]
            [gitcloj.constants :as cns]))
(def ^:const root cns/c-root)
(defn list-branches
  [parent-dir]
  (seq (.list (io/file (str parent-dir root "/refs/heads/")))))

(defn branch?
  [parent-dir p]
  (println (list-branches parent-dir))
  (if (some #(= p %) (list-branches parent-dir))
    true
    false))

(defn build-snapshot
  "Creates the snapshot of working directory with staged & already added files"
  ([parent-dir treehash]
   (build-snapshot parent-dir parent-dir treehash))
  ([parent-dir curr-dir treehash]
   (let [fcontent (obj/cat-file parent-dir treehash)
         lines (cstr/split-lines fcontent)
         parlines (remove nil? (map obj/line-read lines))
         ]
     (let [trees (filter #(= (second %) "tree") parlines)
           blobs (filter #(= (second %) "blob") parlines)
           ]
       (if (nil? trees)
         (apply merge (map #(hash-map (str (cstr/replace-first curr-dir parent-dir "")
                                           (nth % 3)) (nth % 2)) blobs))
         (merge
           (apply merge (map #(hash-map (str (cstr/replace-first curr-dir parent-dir "")
                                             (nth % 3)) (nth % 2)) blobs))
           (apply merge (map #(build-snapshot parent-dir (str (cstr/replace-first curr-dir parent-dir "")
                                                              (nth % 3) "/") (nth % 2))
                             trees))))
       ))))

(defn switch-branch
  "Changes the curret branch ref to point at provided branch checkout"
  [parent-dir bname]
  (spit (rd/get-head-path parent-dir) {:ref (str "refs/heads/" bname)}))

(defn create-new-branch
  [parent-dir brname]
  (let [curr (rd/get-head-hash parent-dir)]
    (if (nil? curr)
      (switch-branch parent-dir brname)
      (do
        (spit (str parent-dir root "refs/heads/" brname) curr)))))

(defn delete-tracked
  "Delete the files & dirs tracked under index"
  [parent-dir]
  (let [snapshot (rd/get-curr-snapshot parent-dir)
        files (keys snapshot)
        ]
    (for [file files]
      (do
        (try
          (io/delete-file (io/file (str parent-dir file)) false)
          (catch Exception e
            (println e)
            nil))
        (loop [p file]
          (if (cstr/includes? p "/")
            (do
              (try
                (io/delete-file (io/file (str parent-dir (subs file 0 (cstr/last-index-of file "/")))) false)
                (catch Exception e
                  (println e)
                  nil))
              (recur (subs file 0 (cstr/last-index-of file "/"))))
            nil)
          )
        ))))


;(defn delete-tracked
;  "Delete the files & dirs tracked under index"
;  [path]
;  (do
;    (try
;      (io/delete-file (io/file path))
;      (catch Exception e
;        nil))
;    (io/delete-file (io/file (subs path 0 (cstr/last-index-of path "/")))))
;    )