(ns job-queue.core
  (:require [cheshire.core :as cheshire]
            [job-queue.repository :refer :all]))

(def agent-store (->AppStore (ref #{})))
(def job-store (->AppStore (ref #{})))

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
    (remove-data agent-store #(= (% :id) agent-id))
    (remove-data job-store #(= (% :id) job-id)))
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
  "Gets an agent by its `id`"
  [id]
  (get-some agent-store #(and (= (% :id) id) %)))


(defn get-available-job 
  "Gets an available job in a specific `jobs` collection given the `types`"
  [jobs types]
  (if (or (empty? types) (empty? jobs))
    nil
    (let [job (first (filter #(= (% :type) (first types)) jobs))]
      (if-not (nil? job)
        job
        (recur jobs (rest types))))))


(defn process-job-request
  "Process a job `request` by allocating an agent to a job"
  [request]

  ; get the agent by id
  (def agent-id (:agent_id request))
  (def agent (get-agent-by-id agent-id))

  ; check whether an agent was returned
  (if (nil? agent)
    nil
    (do
      ; gather the agent's primary and secondary skillset
      (def primary_skillset (:primary_skillset agent))
      (def secondary_skillset (:primary_skillset agent))
      
      ; gather urgent and lower priority jobs from the repository
      (def urgent-jobs (get-data job-store #(= (% :urgent) true)))
      (def minor-jobs (get-data job-store #(= (% :urgent) false)))

      ; seek for an urgent and lower priority available job
      (def urgent-job (get-available-job urgent-jobs (concat (agent :primary_skillset) (agent :secondary_skillset))))
      (def minor-job (get-available-job minor-jobs (concat (agent :primary_skillset) (agent :secondary_skillset))))
      
      ; there is an urgent job? take it! otherwise take a lower priority one
      (def job (if-not (nil? urgent-job) urgent-job minor-job))

      ; get the job id
      (def job-id (:id job))
      
      ; alocate an agent to a job
      (alocate-agent-job agent-id job-id)

      ; returns a formatted job request
      (get-job-request agent-id job-id))))


(defn store-agents
  "Stores all agents specified in `content` in the agent store"
  [content]
  (push-data agent-store (distinct (map #(:new_agent %) (filter #(:new_agent %) content))))
  (get-data agent-store))

(defn store-jobs
  "Stores all agents specified in `content` in the job store"
  [content]
  (push-data job-store (distinct (map #(:new_job %) (filter #(:new_job %) content))))
  (get-data job-store))

(defn process-content 
  "Process the `content` parsed from a json."
  [content]

  ; forces a break in the main thread to avoid error in the STDIN buffer
  (Thread/sleep 500)

  ; gathers and registers the agents and jobs passed in the queue message
  (store-agents content)
  (store-jobs content)

  ; loop over job requests
  (loop [[job-request & requests] (map #(:job_request %) (filter #(:job_request %) content))
        processed-jobs []]
    (if (nil? job-request) 
      processed-jobs
      (recur requests (conj processed-jobs (process-job-request job-request))))))


(defn -main
  "Main function thread"
  [& args]
  (println "\n\nInserts or paste the content to be processed:")
  (let [content (read-json-content)
        processed (process-content content)]
      
      (println "\n\nOutput message:")
      (println (cheshire/encode processed)))
  (recur :take-next-read))
