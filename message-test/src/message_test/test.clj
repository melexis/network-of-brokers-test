(ns message-test.test
  (:require [message-test.activemq :as a]
            [clojure.core.async :refer [<!! chan close!]]))

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
  (let [kill-chan (chan 1)
        init-chan (chan 1)]
    (.start
     (Thread. 
      (fn []
        (println "Starting listener thread for uri" uri)
        (a/with-connection c uri
          (a/with-session s c false :auto_acknowledge
            (a/subscribe s "topic" 
                         (fn [msg] 
                           (send messages-agent conj msg)))

                                        ; Signal that the listener is ready
            (close! init-chan)

            ; Block till the chan is quit
            (while (not (nil? (<!! kill-chan)))
              (Thread/sleep 100)))))))
    
    ; Block till initialization is ready
    (<!! init-chan)
    
    kill-chan))

(defn send-to-multiple-brokers
  [amount & uris]
  (let [messages (map (fn [_] (agent [])) uris)
        uri-messages (zipmap uris messages)]

    ; Start a listener for all uris
    (let [kill-chans (-> (for [[uri messages] uri-messages] 
                           (listener-thread uri messages))
                         (doall))]

                                        ; Send the requested amount of messages
      (-> (for [i (range amount)]
            (let [n (* (count uris) (Math/random))
                  uri (nth uris n)]
              (println "Sending to uri" uri)            
              (a/with-connection c uri
                (a/with-session s c false :auto_acknowledge
                  (let [msg (a/create-message s (str (int i)))]

                    (a/send s "topic" msg :topic))))))
          (doall))

                                        ; Verify that all listeners got the same amount of messages
      (doall 
       (loop [n 0]
         
         (println "After" n "seconds:")
         (-> (for [[uri msgs] uri-messages]
               (do
                 (println "Uri" uri "messages" (count @msgs))
                 (println (map #(.getText %) @msgs))))
             (doall))

         (if (not (every? (fn [[_ msgs]] (= amount (count @msgs)))
                          uri-messages))
           (do
             (Thread/sleep (* 1000 n))
             (recur (inc n))))))

      (-> kill-chans
          (map (fn [c] (close! c)))
          (doall)))))



