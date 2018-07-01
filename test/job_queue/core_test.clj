(ns job-queue.core-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as cheshire]
            [clojure.java.io :as io]
            [job-queue.repository :refer :all]
            [job-queue.core :as core]))

(def agent-store core/agent-store)
(def job-store core/job-store)

; fixture to garantee no repository content
(use-fixtures :each (fn [f]
  (set-data agent-store [])
  (set-data job-store [])
  (f)))

; helper function to read json data
(defn read-json 
  "Read a json content from a `filename`"
  [filename]
  (cheshire/parse-stream
    (clojure.java.io/reader (io/resource filename))
    (fn [k] (keyword k))))

(deftest get-job-request
  (testing "Should format a job request based on agent and job ids"
    (let [expected { :job_assigned { :job_id "job-test-9001" :agent_id "agent-test-0001" }}]
      (is (= (core/get-job-request "agent-test-0001" "job-test-9001") expected)))))

(deftest get-agent-by-id
  (testing "Should return an agent by a specific id"
    (let [input [
                  { 
                    :id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                    :name "BoJack Horseman",
                    :primary_skillset ["bills-questions"],
                    :secondary_skillset []
                  }
                  { 
                    :id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88",
                    :name "Mr. Peanut Butter",
                    :primary_skillset ["rewards-question"],
                    :secondary_skillset ["bills-questions"]
                  }
              ]
          expected {
            :id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88",
            :name "Mr. Peanut Butter",
            :primary_skillset ["rewards-question"],
            :secondary_skillset ["bills-questions"]
          }]
      (set-data core/agent-store input)
      (is (= (core/get-agent-by-id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88") expected)))))

(deftest test-input-1
  (testing "Should pass test input 1"
    (let [actual (core/process-content (read-json "expected-input-1.json"))
          expected (read-json "expected-output-1.json")]
      (is (= actual expected)))))

(deftest test-input-2
  (testing "Should pass test input 2"
    (let [actual (core/process-content (read-json "expected-input-2.json"))
          expected (read-json "expected-output-2.json")]
      (is (= actual expected)))))

(deftest test-input-3
  (testing "Should pass test input 3"
    (let [actual (core/process-content (read-json "expected-input-3.json"))
          expected (read-json "expected-output-3.json")]
      (is (= actual expected)))))

(deftest test-input-4
  (testing "Should pass test input 4"
    (let [actual (core/process-content (read-json "expected-input-4.json"))
          expected (into [] (read-json "expected-output-4.json"))]
      (is (= actual expected)))))

(deftest test-duplicated-data
  (testing "There should not be duplicated data in the repositories"
    (let [json-data (read-json "expected-input-duplicates.json")
          actual-agents (core/store-agents json-data)
          actual-jobs (core/store-jobs json-data)]
      (is (and
        (= (count actual-agents) 1)
        (= (count actual-jobs) 2))))))
