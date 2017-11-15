(ns gitcloj.core
  (:gen-class)
  (require [clojure.string :as cstr]
           [clojure.java.io :as io]
           [gitcloj.reader :as rd]
           [gitcloj.helper :as hp]))

(def ^:const root ".clogit/")
(def ^:const headfile (str root "Head"))
(defn create-directories
  [parent]
  (do
    (let [par (str parent root)]
      (io/make-parents (str par "branches/nil"))
      (io/make-parents (str par "refs/nil"))
      (io/make-parents (str par "refs/heads/nil"))
      (spit (str par "index") "#{}")                           ;index for versioned files
      (spit (str par "Head") {:current                      ;contains staged files
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
  [parent-dir fpath]
  (let [head (rd/get-head parent-dir root) fls (get-in head [:current :files])]
    ;(println head fpath)
    (if (= "." fpath)
      (let [fileset  (apply hash-set (hp/read-relative-paths parent-dir))]
        (spit (str parent-dir headfile)
              (update-in
                head [:current :files] #(apply merge % fileset))))
      (spit (str parent-dir headfile)
            (update-in
              head [:current :files] conj fpath)))))

(defn status
  [parent]
  (let [head (rd/get-head parent root) files (get-in head [:current :files])]
    (println "Current branch: " (get-in head [:current :name]))
    (println "Changes to be committed:" files)
    (println "Changes not staged for commit")
    )
  (let [untracked (hp/get-untracked-files parent root)]
    (when (not-empty untracked)
      (println "Untracked files: ")
      (loop [[i & res] (seq untracked)]
        (when i
          (println "      " i)
          (recur res)))
      ))
  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
