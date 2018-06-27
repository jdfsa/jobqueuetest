(ns job-queue.core
  (:require [clojure.data.json :as json]))

(defn read-json-content []
  (json/read (java.io.BufferedReader. (java.io.InputStreamReader. System/in))))

(defn process-content
  [job-data]
  (loop [job-data job-data]
    (when-not (empty? job-data)
      (println (first job-data))
      (recur (rest job-data)))))

(defn -main [& args]
  (println "\n\nIMPORT JOBS...")
  (let [content (read-json-content)]
    (process-content content)
    (recur :continue)))