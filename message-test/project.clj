(defproject message-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.activemq/activemq-core "5.5.1"]
                 [org.slf4j/slf4j-api "1.5.11"]
                 [org.slf4j/slf4j-simple "1.5.11"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.cli "0.3.1"]
                 [etcd-clojure "0.2.1"]
                 [clj-time "0.8.0"]]
  :main message-test.nagios)
