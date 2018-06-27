(ns job-queue.core
  (:require [clojure.data.json :as json]))

(def prevcontent (atom ""))
(def content (atom ""))

(defn readcontent 
  "Read in content"
  [& args]
  
  (println "Import jobs")

  (doseq [ln (line-seq (java.io.BufferedReader. *in*))]
    (reset! content ln))
)

(defn writecontent
  [& args]
  (loop [a []]
    (Thread/sleep 1000)
    (when-not (= @content @prevcontent)
      (reset! prevcontent @content)
      (println "PRINTANDO A PARADA" @content))
    (recur a))
  )

(defn -main []

  (.start (Thread. (fn [] (readcontent))))
    
  (.start (Thread. (fn [] (writecontent))))
)