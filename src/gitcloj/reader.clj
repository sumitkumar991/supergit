(ns gitcloj.reader
  (require [clojure.java.io :as io]))
(defn get-head
  [parent]
  (read-string (slurp (str parent ".clogit/Head"))))
