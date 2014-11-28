(ns message-test.test
  (:require [message-test.activemq :as a]))

(defn same-broker-test
  []
  (a/with-connection c "tcp://localhost:6000" 
    (a/with-session s c false :auto_acknowledge
      (let [msg (a/create-message s "hello world")] 
        (a/send s "test" msg :queue) 
        (let [msg (a/receive s "test" 1000 :queue)]
          (assert msg))))))

(defn different-broker
  []
  (a/with-connection c "tcp://localhost:6000" 
    (a/with-session s c false :auto_acknowledge
      (let [msg (a/create-message s "hello world")] 
        (a/send s "different-broker" msg :queue))))

  (a/with-connection c "tcp://localhost:6001"
    (a/with-session s c false :auto_acknowledge
      (assert (a/receive s "different-broker" 1000 :queue)))))



