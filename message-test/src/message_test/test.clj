(ns message-test.test
  (:require [message-test.activemq :as a]
            [clojure.core.async :refer [<!! chan close!]]
            [etcd-clojure.core :as etcd]
            [message-test.iptables :as iptables]))

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
  [uri messages-agent destination destination-type]
  (let [kill-chan (chan 1)
        init-chan (chan 1)]
    (.start
     (Thread. 
      (fn []
        (println "Starting listener thread for uri" uri)
        (a/with-connection c uri
          (a/with-session s c false :auto_acknowledge
            (println "Subscribing to" destination-type destination "on uri" uri)
            (a/subscribe s 
                         destination
                         (fn [msg] (send messages-agent conj msg))
                         destination-type)

                                        ; Signal that the listener is ready
            (close! init-chan)

                                        ; Block till the chan is quit
            (while (not (nil? (<!! kill-chan)))
              (Thread/sleep 100)))))))
    
                                        ; Block till initialization is ready
    (<!! init-chan)
    
    kill-chan))

(defn ^:private interruption-thread
  [max-sleep-time]
  (let [zipmap* (fn [xs] (zipmap xs (repeat :accept)))
        endpoints (->> (etcd/list "ip")
                       (map :value)
                       (zipmap*)
                       (atom))
        kill-chan (chan 1)
        kill-chans (map (fn [_] (chan 1)) @endpoints)
        killed-chan (chan 1)
        continue (atom true)]
    (doall (map (fn [[ip _] n c]
                  (.start 
                   (Thread.
                    (fn []
                      (loop [status (get @endpoints ip)]
                        (println "Endpoint" ip "is now in status" status)
                        (let [sleep-time (-> (Math/random) (* max-sleep-time) (int))
                              type (if (= :accept status) :drop :accept)]
                          (Thread/sleep sleep-time)

                          (println "Setting rule" :forward n :tcp ip "61616" type)
                          (iptables/set-rule :forward n :tcp ip "61616" type)
                          (swap! endpoints assoc ip type)
                          (if @continue
                            (recur type)
                            (do
                                        ; Allow communication again before stopping
                              (if (= type :drop)
                                (iptables/set-rule :forward n :tcp ip "61616" :accept))
                              (close! c)))))))))
                @endpoints
                (map (partial inc) (range (count @endpoints)))
                kill-chans))

    (.start
     (Thread.
      (fn []

                                        ; Wait till we're signalled to stop
        (<!! kill-chan)
        (reset! continue false)

                                        ; Wait till all threads are stopped
        (doseq [kc kill-chans] (<!! kc))

                                        ; Singnal that we're completely stopped
        (close! killed-chan))))
    [kill-chan killed-chan endpoints]))

(defn send-to-multiple-brokers
  [amount & uris]
  (let [messages (map (fn [_] (agent [])) uris)
        uri-messages (zipmap uris messages)]

                                        ; Start a listener for all uris
    (let [kill-chans (-> (map (fn [[uri messages]]
                                (let [destination-name (str "Consumer." (.toString (java.util.UUID/randomUUID)) ".VirtualTopic.topic")]
                                  (listener-thread uri messages destination-name :queue))) 
                              uri-messages)                     
                         (doall))
                                        ; Start the interruption threads that will randomly drop messages
          [interruption-kill-chan interruption-killed-chan interruption-endpoints] (interruption-thread 20000)]

                                        ; Send the requested amount of messages
      (-> (for [i (range amount)]
            (let [n (* (count uris) (Math/random))
                  uri (nth uris n)
                  ;uri (nth uris 2)
                  ]
              (a/with-connection c uri
                (a/with-session s c false :auto_acknowledge
                  (let [msg (a/create-message s (str (int i)))]
                    (println "Sending message" i "to uri" uri)
                    (a/send s "VirtualTopic.topic" msg :topic))))))
          (doall))

                                        ; Sending is completed,  signal the interruption thread to stop
      (close! interruption-kill-chan)
      (println "Waiting for the interruption kill chan to stop")
      (<!! interruption-killed-chan)

                                        ; Verify that all listeners got the same amount of messages
      (doall 
       (loop [n 0]
         
         (println "After" n "seconds:")
         (-> (for [[uri msgs] uri-messages]
               (do
                 (println "Uri" uri "messages" (count @msgs))
                 (println "Endpoints:" @interruption-endpoints)
                 (println "Duplicates:" (->> @msgs
                                             (group-by identity)
                                             (filter #(> (count (second %)) 1))
                                             (map first)))
                 
                 ;(println (map #(.getText %) @msgs))
                 ))
             (doall))

         (if (not (every? (fn [[_ msgs]] (= amount (count @msgs)))
                          uri-messages))
           (do
             (Thread/sleep 1000)
             (recur (inc n))))))

      (->> kill-chans
           (map (fn [c] (close! c)))
           (doall)))))

(defn test-servers-test []
  (send-to-multiple-brokers 10 "failover:(tcp://esb-a-test.sensors.elex.be:61602,tcp://esb-b-test.sensors.elex.be:61602)" "failover:(tcp://esb-a-test.sofia.elex.be:61602,tcp://esb-b-test.sofia.elex.be:61602)" "failover:(tcp://esb-a-test.erfurt.elex.be:61602,tcp://esb-b-test.erfurt.elex.be:61602)" "failover:(tcp://esb-a-test.colo.elex.be:61602,tcp://esb-b-test.colo.elex.be:61602)" "failover:(tcp://esb-a-test.kuching.elex.be:61602,tcp://esb-b-test.kuching.elex.be:61602)"))
