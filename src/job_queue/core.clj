(ns job-queue.core
  (:require [cheshire.core :as cheshire]))

(defn read-json-content []
  (cheshire/parse-stream 
    (java.io.BufferedReader. (java.io.InputStreamReader. System/in))
    (fn [k] (keyword k))))

(def agents-repository (ref #{}))
(def jobs-repository (ref #{}))
(def job-requests-repository (ref #{}))

(defn push [repository item] (dosync (alter repository conj item)))

(defn register-job-request [job agent] 
  (let [job-id (job :id) 
        agent-id (agent :id)
        job-request {
          :job_assigned {
            :job_id job-id
            :agent_id agent-id
          }
        }]
    (dosync 
      (alter job-requests-repository conj job-request)
      (ref-set agents-repository (into #{} (remove #(= (% :id) agent-id) @agents-repository)))
      (ref-set jobs-repository (into #{} (remove #(= (% :id) job-id) @jobs-repository)))
    )))

(defn get-agent-by-id [agents id]
  (into {} (filter #(= (% :id) id) agents)))

(defn get-available-job [jobs types]
  (if (or (empty? types))
    nil
    (do 
      (def job (first (filter #(= (% :type) (first types)) jobs)))
      (if-not (nil? job)
        job
        (recur jobs (rest types))))))

(defn process-job-request [job-request]
  (def agent-id (job-request :agent_id))
  (def agent (get-agent-by-id @agents-repository agent-id))
  (def primary_skillset (agent :primary_skillset))
  (def secondary_skillset (agent :primary_skillset))
  
  (def urgent-jobs (filter #(= (% :urgent) true) @jobs-repository))
  (def minor-jobs (filter #(= (% :urgent) false) @jobs-repository))

  (def urgent-job (get-available-job urgent-jobs (concat (agent :primary_skillset) (agent :secondary_skillset))))
  (def minor-job (get-available-job minor-jobs (concat (agent :primary_skillset) (agent :secondary_skillset))))

  (def job (if-not (nil? urgent-job) urgent-job minor-job))

  (register-job-request job agent))

(defn process-content [job-data]
    
  (loop [job-data job-data]
    (let [item (first job-data)]      
      (when-not (empty? job-data)
        
        (def new-agent (item :new_agent))
        (def new-job (item :new_job))
        (def job-request (item :job_request))

        (when-not (nil? new-agent) (push agents-repository new-agent) :new-agent)
        (when-not (nil? new-job) (push jobs-repository new-job) :new-job)
        (when-not (nil? job-request) (process-job-request job-request))
        
        (recur (rest job-data))))))

(defn -main [& args]
  (println "\n\nIMPORT QUEUE...")
  (let [content (read-json-content)]
      (process-content content)
      (println (cheshire/encode @job-requests-repository)))
  (recur :continue))