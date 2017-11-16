(ns gitcloj.reader
  (require [clojure.java.io :as io]))

(defn get-head
  [parent root]
  (read-string (slurp (str parent root "Head"))))

(defn get-head-files
  "returns the set of staged files"
  [parent-dir root]
  (get-in (get-head parent-dir root) [:current :files]))