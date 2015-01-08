#!/bin/bash

# This script monitors the status of the network connectors of the global network of brokers
# and automatically restarts them when needed.

LOG=/var/log/network-of-brokers-prod.log

DIR=/usr/share/network-of-brokers/

echo "`date` | INFO | Start monitoring network-of-brokers" >> $LOG

while true; do
    echo "`date` | INFO | Start message test" >> $LOG
    java -jar $DIR/message-test-0.1.0-SNAPSHOT-standalone.jar 30000 "failover:(tcp://esb-a.sensors.elex.be:61602,tcp://esb-b.sensors.elex.be:61602)" "failover:(tcp://esb-a.erfurt.elex.be:61602,tcp://esb-b.erfurt.elex.be:61602)" "failover:(tcp://esb-a.sofia.elex.be:61602,tcp://esb-b.sofia.elex.be:61602)" "failover:(tcp://esb-a.kuching.elex.be:61602,tcp://esb-b.kuching.elex.be:61602)" "failover:(tcp://esb-a.colo.elex.be:61602,tcp://esb-b.colo.elex.be:61602)"
    RESULT=$?
    if [ "$RESULT" -ne 0 ]
    then echo "`date` | WARN | restarting network connectors" >> $LOG
	 java -jar $DIR/restart-network-connectors-0.1.0-SNAPSHOT-standalone.jar -e prod -p 1099 -u smx -w Kon6QuaytUc? -j karaf-root -a 5.5
	 echo "`date` | INFO | restarted" >> $LOG
    fi
    echo "`date` | INFO | Sleeping 60 secs" >> $LOG
    sleep 60
done


