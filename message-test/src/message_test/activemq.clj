(ns message-test.activemq
  (:import org.apache.activemq.ActiveMQConnectionFactory
           javax.jms.Session))

(def acknowledge-modes
  {:auto_acknowledge Session/AUTO_ACKNOWLEDGE})

(defmacro with-connection
  [connection uri & body]
  `(let [connection-factory# (ActiveMQConnectionFactory. ~uri)
         connection# (.createConnection connection-factory#)
         ~connection connection#]
    (.start connection#)
    (try
      ~@body
      (finally 
        (.close connection#)))))

(defmacro with-session
  [session connection transacted acknowledge-mode & body]
  `(let [session# (.createSession ~connection ~transacted ~(get acknowledge-modes acknowledge-mode))
         ~session session#]
    (try
      ~@body
      (finally
        (.close session#))))) 

(defmulti send (fn [_ _ _ t] t))

(defmethod send :queue
  [session destination-name msg _]
  (let [destination (.createQueue session destination-name)
        producer (.createProducer session destination)]
    (.send producer msg)))

(defmethod send :topic
  [session destination-name msg _]
  (let [destination (.createTopic session destination-name)
        producer (.createProducer session destination)]
    (.send producer msg)))

(defmulti create-message (fn [_ body] (type body)))

(defmethod create-message java.lang.String
  [session body]
  (.createTextMessage session body))

(defmulti receive (fn [_ _ _ t] t))

(defmethod receive :queue
  [session destination-name timeout _]
  (let [destination (.createQueue session destination-name)
        consumer (.createConsumer session destination)]
    (.receive consumer timeout)))

(defmethod receive :topic
  [session destination-name timeout _]
  (let [destination (.createTopic session destination-name)
        consumer (.createConsumer session destination)]
    (.receive consumer timeout)))

(defmulti subscribe (fn [_ _ _ type] type))

(defmethod subscribe :queue
  [session queue on-message-cb _]
  (let [destination (.createQueue session queue)
        consumer (.createConsumer session destination)]
    (.setMessageListener consumer
                         (reify
                           javax.jms.MessageListener
                           (onMessage [_ msg] (on-message-cb msg))))))

(defmethod subscribe :topic
  [session topic on-message-cb _]
  (let [destination (.createTopic session topic)
        consumer (.createConsumer session destination)]
    (.setMessageListener consumer
                         (reify 
                           javax.jms.MessageListener
                           (onMessage [_ msg] (on-message-cb msg))))))
