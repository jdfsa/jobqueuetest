(ns job-queue.core
  (:require [clojure.data.json :as json]))

(defn read-json-content []
  (json/read (java.io.BufferedReader. (java.io.InputStreamReader. System/in))))

(def agents-repository (ref {}))
(def jobs-repository (ref {}))

(defn push [item repository]
  (dosync (alter repository conj (deref repository) item)))

(defn process-content
  [job-data]
  
  (loop [job-data job-data]
    (let [first-item (first job-data)]      
      (when-not (empty? job-data)
        
        (def new-agent (first-item "new_agent"))
        (def new-job (first-item "new_job"))

        (println "debug: " (first-item "new_job"))
        
        (when-not (nil? new-agent) (push new-agent agents-repository) :new-agent)
        (when-not (nil? new-job) (push new-job jobs-repository) :new-job)
        
        (recur (rest job-data))))))

(defn -main [& args]
  (println "\n\nIMPORT JOBS...")
  (let [content (read-json-content)]
    (process-content content)
    (recur :continue)))