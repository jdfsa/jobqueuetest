(ns job-queue.core-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as cheshire]
            [clojure.java.io :as io]
            [job-queue.core :refer :all]))

(defn read-json [filename]
  (cheshire/parse-stream
    (clojure.java.io/reader (io/resource filename))
    (fn [k] (keyword k))))

(deftest test-input-1
  (testing "Should pass test input 1"
    (let [actual (process-content (read-json "expected-input-1.json"))
          expected (read-json "expected-output-1.json")]
      (is (= actual expected)))))

(deftest test-input-2
  (testing "Should pass test input 2"
    (let [actual (process-content (read-json "expected-input-2.json"))
          expected (read-json "expected-output-2.json")]
      (is (= actual expected)))))
