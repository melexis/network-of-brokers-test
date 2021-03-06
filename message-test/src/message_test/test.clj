(ns message-test.test
  (:require [message-test.activemq :as a]
            [message-test.message :refer [attach-agent-to-uris listen-for-messages send-messages]]
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


(defn ^:private receive-messages [amount uri-messages]
    (doall 
     (loop [n 0]
       (println "After" n "seconds:")
       (-> (for [[uri msgs] uri-messages]
             (do
               (println "Uri" uri "messages" (count @msgs))
               (println "Duplicates:" (->> @msgs
                                           (group-by identity)
                                           (filter #(> (count (second %)) 1))
                                           (map first)))))
           (doall))

       (if (not (every? (fn [[_ msgs]] (= amount (count @msgs)))
                        uri-messages))
         (do
           (Thread/sleep 1000)
           (recur (inc n)))))))



(defn test-servers [env]
  (let [env* (cond
              (= env "test") "-test"
              (= env "uat") "-uat"
              (= env "prod") ""
              :default (throw (Exception. "Invalid environment")))
        brokers (for [site ["sensors" "sofia" "erfurt" "colo" "kuching"]]
                  (let [nodeA (clojure.string/join "." [(str "tcp://esb-a" env*) site "elex" "be:61602"])
                        nodeB (clojure.string/join "." [(str "tcp://esb-b" env*) site "elex" "be:61602"])]
                    (str "failover:(" nodeA "," nodeB ")")))]
    (apply send-to-multiple-brokers 10 {:interrupt? false} brokers)))



