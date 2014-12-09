(ns message-test.test
  (:require [message-test.activemq :as a]))

(defn same-broker-test
  []
  (a/with-connection c "tcp://localhost:7000" 
    (a/with-session s c false :auto_acknowledge
      (let [msg (a/create-message s "hello world")] 
        (a/send s "test" msg :queue) 
        (let [msg (a/receive s "test" 1000 :queue)]
          (assert msg))))))

(defn different-broker
  []
  (a/with-connection c "tcp://localhost:7000" 
    (a/with-session s c false :auto_acknowledge
      (let [msg (a/create-message s "hello world")] 
        (a/send s "different-broker" msg :queue))))

  (a/with-connection c "tcp://localhost:7001"
    (a/with-session s c false :auto_acknowledge
      (assert (a/receive s "different-broker" 1000 :queue)))))

(defn ^:private listener-thread
  [uri messages-agent]
  (.start
   (Thread. 
    (fn []
      (a/with-connection c uri
        (a/with-session s c false :auto_acknowledge
          (while true
            (when-let [msg (a/receive s "topic" 100 :topic)]
              (send messages-agent conj msg)))))))))

(defn send-to-multiple-brokers
  [amount]
  (let [site-a-messages (agent [])
        site-b-messages (agent [])
        site-c-messages (agent [])
        uris ["tcp://localhost:7000" "tcp://localhost:7001" "tcp://localhost:7002"]]
    (listener-thread "tcp://localhost:7000" site-a-messages)
    (listener-thread "tcp://localhost:7001" site-b-messages)
    (listener-thread "tcp://localhost:7002" site-c-messages)

    (-> (for [i (range amount)]
          (let [n (* (count uris) (Math/random))
                uri (nth uris n)]
            (a/with-connection c uri
              (a/with-session s c false :auto_acknowledge
                (let [msg (a/create-message s (str (int n)))]
                  (a/send s "topic" msg :topic)
                  (Thread/sleep 10))))))
        (doall))

    
    (doall 
     (loop [n 0]
       (println "After" n "seconds:" (count @site-a-messages) (count @site-b-messages) (count @site-c-messages))
       (Thread/sleep (* 1000 n))
       (if (not (and (= amount (count @site-a-messages))
                     (= amount (count @site-b-messages))
                     (= amount (count @site-c-messages))))
         (recur (inc n)))))

    (assert (= amount (count @site-a-messages)))
    (assert (= amount (count @site-b-messages)))
    (assert (= amount (count @site-c-messages)))))



