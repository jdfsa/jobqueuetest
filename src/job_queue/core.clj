(ns job-queue.core
  (:require [cheshire.core :as cheshire]))

(def agents-repository (ref []))
(def jobs-repository (ref ()))
(defn set-data [repository content] (dosync (ref-set repository content)))
(defn erase [repository] (dosync (ref-set repository #{})))

(defn read-json-content 
  "Read a json content passed via STDIN using the cheshire library"
  []
  (cheshire/parse-stream 
    (java.io.BufferedReader. (java.io.InputStreamReader. System/in))
    (fn [k] (keyword k))))

(defn alocate-agent-job
  "Marks an agent and a job through their ids as alocated to a job request 
  by removing them from the respective available lists."
  [agent-id job-id]
  (dosync 
    (ref-set agents-repository (into [] (remove #(= (% :id) agent-id) @agents-repository)))
    (ref-set jobs-repository (into [] (remove #(= (% :id) job-id) @jobs-repository))))
  true)

(defn get-job-request
  "Gets a formatted job request by taking a `job` and an `agent`."
  [agent-id job-id] 
  { 
    :job_assigned { 
      :job_id job-id
      :agent_id agent-id 
    } 
  })

(defn get-agent-by-id 
  "Gets an agent by its `id` given an `agents` collection"
  [agents id]
  (some #(and (= (% :id) id) %) agents))

(defn get-available-job [jobs types]
  (if (empty? types)
    nil
    (do 
      (def job (first (filter #(= (% :type) (first types)) jobs)))
      (if-not (nil? job)
        job
        (recur jobs (rest types))))))

(defn process-job-request
  "Process a job `request` by allocating an agent to a job"
  [request]

  ; get the agent by id
  (def request-value (:agent_id request))
  (def agent (get-agent-by-id @agents-repository request-value))

  ; gather the agent's primary and secondary skillset
  (def primary_skillset (:primary_skillset agent))
  (def secondary_skillset (:primary_skillset agent))
  
  ; gather urgent and lower priority jobs from the repository
  (def urgent-jobs (filter #(= (% :urgent) true) @jobs-repository))
  (def minor-jobs (filter #(= (% :urgent) false) @jobs-repository))

  ; seek for an urgent and lower priority available job
  (def urgent-job (get-available-job urgent-jobs (concat (agent :primary_skillset) (agent :secondary_skillset))))
  (def minor-job (get-available-job minor-jobs (concat (agent :primary_skillset) (agent :secondary_skillset))))
  
  ; there is an urgent job? take it! otherwise take a lower priority one
  (def job (if-not (nil? urgent-job) urgent-job minor-job))

  ; get the agent and job ids
  (def agent-id (:id agent))
  (def job-id (:id job))
  
  ; alocate an agent to a job
  (alocate-agent-job agent-id job-id)

  ; returns a formatted job request
  (get-job-request agent-id job-id))

(defn process-content 
  "Process the `content` parsed from a json."
  [content]

  ; forces a break in the main thread to avoid error in the STDIN buffer
  (Thread/sleep 500)

  ; gathers and registers the agents and jobs passed in the queue message
  (set-data agents-repository (into [] (map #(:new_agent %) (filter #(:new_agent %) content))))
  (set-data jobs-repository (into [] (map #(:new_job %) (filter #(:new_job %) content))))

  ; loop over job requests
  (loop [[job-request & requests] (map #(:job_request %) (filter #(:job_request %) content))
        processed-jobs []]
    (if (nil? job-request) 
      processed-jobs
      (recur requests (conj processed-jobs (process-job-request job-request))))))

(defn -main
  "Main function thread"
  [& args]
  (println "\n\nINPUT QUEUE...")
  (let [content (read-json-content)
        processed (process-content content)]
      
      (println "\nOUTPUT QUEUE:")
      (println (cheshire/encode processed)))
  (recur :take-next-read))