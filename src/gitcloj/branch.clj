(ns gitcloj.branch
  (:require [clojure.java.io :as io]
            [clojure.string :as cstr]
            [gitcloj.objects :as obj]
            [gitcloj.reader :as rd]))

(defn list-branches
  [parent-dir root]
  (seq (.listFiles (io/file (str parent-dir root "/refs/heads/")))))

(defn branch?
  [parent-dir root p]
  (if (some #(= p %) (list-branches parent-dir root))
    true
    false))

(defn build-snapshot
  "Creates the snapshot of working directory with staged & already added files"
  ([parent-dir root treehash]
   (build-snapshot parent-dir root parent-dir treehash))
  ([parent-dir root curr-dir treehash]
   (let [fcontent (obj/cat-file parent-dir root treehash)
         lines (cstr/split-lines fcontent)
         parlines (remove nil? (map obj/line-read lines))
         ]
     (let [trees (filter #(= (second %) "tree") parlines)
           blobs (filter #(= (second %) "blob") parlines)
           ]
       (if (nil? trees)
         (apply merge (map #(hash-map (str curr-dir (nth % 3)) (nth % 2)) blobs))
         (merge
           (apply merge (map #(hash-map (str curr-dir (nth % 3)) (nth % 2)) blobs))
           (apply merge (map #(build-snapshot parent-dir root (str curr-dir (nth % 3) "/") (nth % 2))
                             trees))))
       ))))

(defn switch-branch
  "Changes the curret branch ref to point at new checkout"
  [parent-dir root bname]
  (spit (rd/get-head-path parent-dir root) {:ref (str "refs/heads/" bname)}))

(defn create-new-branch
  [parent-dir root brname]
  (let [curr (rd/get-head-hash parent-dir root)]
    (if (nil? curr)
      (switch-branch parent-dir root brname)
      (do
        (spit (str parent-dir root "refs/heads/" brname) curr)
        (switch-branch parent-dir root brname)))))