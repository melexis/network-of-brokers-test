(ns message-test.iptables
  (:require [clojure.java.shell :as shell]))

(defn set-rule
  [rule seq protocol source-ip source-port target]
  (let [rule* (clojure.string/upper-case (name rule))
        protocol* (name protocol)
        target* (clojure.string/upper-case (name target))]
    (println "sudo" "-S" "/sbin/iptables" "-R" rule* seq "-p" protocol* "-s" source-ip "--sport" source-port "-j" target* :in " \n")
    (let [{:keys [exit err]} (shell/sh "sudo" "-S" "/sbin/iptables" "-R" rule* (str seq) "-p" protocol* "-s" source-ip "--sport" (str source-port) "-j" target*  :in "**secret**\n")]
      (if (not= 0 exit)
        (println err))
      )
    ))
