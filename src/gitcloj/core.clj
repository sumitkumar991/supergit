(ns gitcloj.core
  (:gen-class)
  (require [clojure.java.io :as io])
  (require [gitcloj.reader :as rd]))
(defn read-all-file
  [directory]
  (.listFiles (io/file directory)))
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
(defn add
  [parent fpath]
  (if (= "." fpath)
    "do something here"
    (let [head (rd/get-head parent) fls (get-in head [:current :files])]
      (println head fpath)
      (spit (str parent headfile)
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
