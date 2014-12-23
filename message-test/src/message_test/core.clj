(ns message-test.core)
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  [["-e" "--environment ENVIRONMENT" "Environment to restart"
    :default "test"
    :validate [#(.contains ["test" "uat" "prod"] %) "Must be one of test, uat, prod"]]])

(defn -main 
  [& args]
  (let [{:keys [options arguments summary errors]} (parse-opts args cli-options)
        {:keys [environment]} options]
    (if errors
      (do (println "Error while parsing arguments")
          (println errors)
          (println summary))
      (message-test.test/test-servers environment))))
