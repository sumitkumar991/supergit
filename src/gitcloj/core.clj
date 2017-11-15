(ns gitcloj.core
  (:gen-class)
  (require [clojure.java.io :as io])
  (require [gitcloj.reader :as rd])
  (require [clojure.string :as cstr]))

(def root ".clogit/")
(def headfile ".clogit/Head")
(defn create-directories
  [parent]
  (do
    (let [par (str parent root)]
      (io/make-parents (str par "branches/nil"))
      (io/make-parents (str par "refs/nil"))
      (io/make-parents (str par "refs/heads/nil") )
      (spit (str par "Head") {:current
                              {
                               :name "master"
                               :files #{}
                               }})
      (spit (str par "branches/Index")
            {"master" #{}})
      (io/make-parents (str par "branches/master/nil")))))
(defn init
  "Checks if current directory is under version control & creates new if not"
  [parent]
  (if (.isDirectory (io/file (str parent root)))
    (println "The Directory is already under vcs")
    (create-directories parent)))

(defn read-files-recursively
  "Helper method of read-relative-paths to get absolute paths of all files from parent directory"
  ([directory]
   (let [
         allfiles (flatten
                    (let [filelist (remove #(cstr/starts-with? % ".clogit") (seq (.list (io/file directory))))]
                      (for [dir filelist]
                        (if (.isDirectory (io/file directory dir))
                          (read-files-recursively (str directory dir "/"))
                          (str directory dir)))))
         ]
     allfiles)))

(defn read-relative-paths
  "Call this method to get a list with relative paths of all the files from the given root directory"
  [parent-dir]
  (map #(cstr/replace-first % parent-dir "") (read-files-recursively parent-dir)))

(defn add
  [parent-dir fpath]
  (let [head (rd/get-head parent-dir) fls (get-in head [:current :files])]
    ;(println head fpath)
    (if (= "." fpath)
      (let [fileset  (apply hash-set (read-relative-paths parent-dir))]
        (spit (str parent-dir headfile)
              (update-in
                head [:current :files] #(apply merge % fileset))))
      (spit (str parent-dir headfile)
            (update-in
              head [:current :files] conj fpath)))))

(defn status
  [parent]
  (let [head (rd/get-head parent) files (get-in head [:current :files])]
    (println "Current branch: " (get-in head [:current :name]))
    (println "Head:" files)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
