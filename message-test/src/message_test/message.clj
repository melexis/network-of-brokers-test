(ns message-test.message
  (:require [message-test.activemq :as a]
            [clojure.core.async :refer [<!! chan close!]]))

(defn attach-agent-to-uris
  [uris]
  (zipmap uris
          (map (fn [_] (agent [])) uris)))

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

(defn listen-for-messages
  [destination-name uri-messages]
  (println "Listen for messages " uri-messages)
  (-> (map (fn [[uri messages]]
             (listener-thread uri messages destination-name :topic))
           uri-messages)
      (doall)))

(defn send-messages [destination amount uris]
                                        ; Send the requested amount of messages
  (-> (for [i (range amount)]
        (let [n (* (count uris) (Math/random))
              uri (nth uris n)]
          (a/with-connection c uri
            (a/with-session s c false :auto_acknowledge
              (let [msg (a/create-message s (str (int i)))]
                (println "Sending message" i "to uri" uri)
                (a/send s destination msg :topic))))))
      (doall)))

