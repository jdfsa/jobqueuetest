(ns job-queue.core
  (:require [cheshire.core :as cheshire]))

(defn read-json-content []
  (cheshire/parse-stream 
    (java.io.BufferedReader. (java.io.InputStreamReader. System/in))
    (fn [k] (keyword k)))
)

(def agents-repository (ref #{}))
(def jobs-repository (ref #{}))
(def job-requests-repository (ref {}))

(defn push [repository item]
  (dosync (alter repository conj item)))

(defn get-agent-by-id [agents id]
  (into {} (filter #(= (% :id) id) agents)))

(defn get-job-by-type [jobs job-type]
  (if (nil? job-type)
    nil
    (into {} (filter #(= (% :type) job-type)))))

(defn process-job-request [job-request]
  (def agent-id (job-request :agent_id))
  (def agent (get-agent-by-id @agents-repository agent-id))
  ; (def job (get-job-by-type @jobs-repository))
  ; (println (first (agent :primary_skillset)))
  ; (println (first (agent :secondary_skillset)))
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
  (println "\n\nIMPORT QUEUE...")
  (let [content (read-json-content)]
      (process-content content))
      (println @agents-repository)
  (recur :continue))