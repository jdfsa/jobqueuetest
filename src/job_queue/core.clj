(ns job-queue.core
  (:require [cheshire.core :as cheshire]))

(defn read-json-content []
  (cheshire/parse-stream 
    (java.io.BufferedReader. (java.io.InputStreamReader. System/in))
    (fn [k] (keyword k))))

(def agents-repository (ref []))
(def jobs-repository (ref ()))

(defn set-data [repository content] (dosync (ref-set repository content)))
(defn erase [repository] (dosync (ref-set repository #{})))

(defn register-job-request [job agent] 
  (let [job-id (:id job) 
        agent-id (:id agent)
        job-request {
          :job_assigned {
            :job_id job-id
            :agent_id agent-id
          }
        }]
    (dosync 
      (ref-set agents-repository (into [] (remove #(= (% :id) agent-id) @agents-repository)))
      (ref-set jobs-repository (into [] (remove #(= (% :id) job-id) @jobs-repository))))
    job-request))

(defn get-agent-by-id [agents id]
  (some #(and (= (% :id) id) %) agents))

(defn get-available-job [jobs types]
  (if (empty? types)
    nil
    (do 
      (def job (first (filter #(= (% :type) (first types)) jobs)))
      (if-not (nil? job)
        job
        (recur jobs (rest types))))))

(defn process-job-request [request]
  (def agent-id (:agent_id request))
  (def agent (get-agent-by-id @agents-repository agent-id))

  (def primary_skillset (:primary_skillset agent))
  (def secondary_skillset (:primary_skillset agent))
  
  (def urgent-jobs (filter #(= (% :urgent) true) @jobs-repository))
  (def minor-jobs (filter #(= (% :urgent) false) @jobs-repository))

  (def urgent-job (get-available-job urgent-jobs (concat (agent :primary_skillset) (agent :secondary_skillset))))
  (def minor-job (get-available-job minor-jobs (concat (agent :primary_skillset) (agent :secondary_skillset))))
  
  (def job (if-not (nil? urgent-job) urgent-job minor-job))

  (register-job-request job agent))

(defn process-content [job-data]
  (Thread/sleep 500)
  (set-data agents-repository (into [] (map #(:new_agent %) (filter #(:new_agent %) job-data))))
  (set-data jobs-repository (into [] (map #(:new_job %) (filter #(:new_job %) job-data))))
  (loop [[job-request & requests] (map #(:job_request %) (filter #(:job_request %) job-data))
        processed-jobs []]
    (if (nil? job-request) 
      processed-jobs
      (recur requests (conj processed-jobs (process-job-request job-request))))))

(defn -main [& args]
  (println "\n\nINPUT QUEUE...")
  (let [content (read-json-content)]
      (def processed (process-content content))
      
      (println "\nOUTPUT QUEUE:")
      (println (cheshire/encode processed))
  )
  (recur :continue))