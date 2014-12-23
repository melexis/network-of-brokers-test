(ns message-test.nagios
  (:require [message-test.message :refer [attach-agent-to-uris listen-for-messages send-messages]]
            [clj-time.core :refer [now plus millis after?]]
            [clojure.core.async :refer [close!]])
  (:gen-class))

(defn all-complete?
  [agents]
  (every? (fn [agent] (<= 10 (count @agent)))
          agents))

(defn run
  [uris timeout]
  (let [uri-agents (attach-agent-to-uris uris)
        deadline (plus (now) (millis timeout))
        kill-chans (listen-for-messages "nagiosTest" uri-agents)]

    (send-messages "nagiosTest" 10 uris)
    
    (let [result (loop []
                   (println uri-agents)
                   (cond
                    (all-complete? (vals uri-agents)) true
                    (after? (now) deadline) false
                    :else (do 
                            (Thread/sleep 1000)
                            (recur))))]
      
      (map (partial close!) kill-chans)

      (if-not result
        (let [not-complete (filter (fn [[uri messages]] (> 10 (count @messages))) uri-agents)]
          (binding [*out* *err*]
            (println "Not all messages arrived for uris" (keys not-complete)))))
      result)))

(defn -main
  [timeout & uris]
  (if (run uris (Integer/parseInt timeout))
    ; the test was successful
    (System/exit 0)
    (System/exit 1)))
