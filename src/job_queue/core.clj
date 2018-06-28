(ns job-queue.core
  (:require [cheshire.core :as cheshire]))

(defn read-json-content []
  (cheshire/parse-stream 
    (java.io.BufferedReader. (java.io.InputStreamReader. System/in))
    (fn [k] (keyword k)))
)

(def agents-repository (ref []))
(def jobs-repository (ref []))
(def job-requests-repository (ref []))

(defn push [repository item]
  (dosync (alter repository conj item)))

(defn get-item-by-id [items id]
  (filter (fn [item] (= (item :id) id)) items))

(defn process-job-request [job-request]
  (def agent-id (job-request :agent_id))
  (def agent (get-item-by-id @agents-repository agent-id))
  (println agent)
  )

(defn process-content
  [job-data]
  
  (loop [job-data job-data]
    (let [first-item (first job-data)]      
      (when-not (empty? job-data)
        
        (def new-agent (first-item :new_agent))
        (def new-job (first-item :new_job))
        (def job-request (first-item :job_request))

        (when-not (nil? new-agent) (push agents-repository new-agent) :new-agent)
        (when-not (nil? new-job) (push jobs-repository new-job) :new-job)
        (when-not (nil? job-request) (process-job-request job-request))
        
        (recur (rest job-data))))))

(defn -main [& args]
  (println "\n\nIMPORT JOBS...")
  (let [content (read-json-content)]
    (process-content content)
    (recur :continue)))