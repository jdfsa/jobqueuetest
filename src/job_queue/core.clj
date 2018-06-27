(ns job-queue.core
  (:require [clojure.data.json :as json]))

(defn read-json-content []
  (json/read (java.io.BufferedReader. (java.io.InputStreamReader. System/in))))

(defn -main [& args]
  (println "IMPORT JOBS...")
  (let [content (read-json-content)]
    (println "teste: " content))
    (recur :continue))