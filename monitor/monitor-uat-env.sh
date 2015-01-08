#!/bin/bash

# This script monitors the status of the network connectors of the global network of brokers
# and automatically restarts them when needed.

LOG=/var/log/network-of-brokers-uat.log

DIR=/usr/share/network-of-brokers/

echo "`date` | INFO | Start monitoring network-of-brokers" >> $LOG

while true; do
    echo "`date` | INFO | Start message test" >> $LOG
    java -jar $DIR/message-test-0.1.0-SNAPSHOT-standalone.jar 30000 "failover:(tcp://esb-a-uat.sensors.elex.be:61602,tcp://esb-b-uat.sensors.elex.be:61602)" "failover:(tcp://esb-a-uat.erfurt.elex.be:61602,tcp://esb-b-uat.erfurt.elex.be:61602)" "failover:(tcp://esb-a-uat.sofia.elex.be:61602,tcp://esb-b-uat.sofia.elex.be:61602)" "failover:(tcp://esb-a-uat.kuching.elex.be:61602,tcp://esb-b-uat.kuching.elex.be:61602)" "failover:(tcp://esb-a-uat.colo.elex.be:61602,tcp://esb-b-uat.colo.elex.be:61602)"
    RESULT=$?
    if [ "$RESULT" -ne 0 ]
    then echo "`date` | WARN | restarting network connectors" >> $LOG
	 java -jar $DIR/restart-network-connectors-0.1.0-SNAPSHOT-standalone.jar -e uat -p 11119 -u admin -w tivEbyie -j jmxrmi -a 5.10
	 echo "`date` | INFO | restarted" >> $LOG
    fi
    echo "`date` | INFO | Sleeping 60 secs" >> $LOG
    sleep 60
done


