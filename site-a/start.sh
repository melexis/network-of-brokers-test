#!/bin/dash

# Publish my ip to etcd

echo "Waiting for etcd to init"

sleep 5

IP=`hostname  -I | cut -f1 -d' '`
HOSTNAME=`hostname`
URI_URL="http://172.17.42.1:4001/v2/keys/uri/$HOSTNAME"
IP_URL="http://172.17.42.1:4001/v2/keys/ip/$HOSTNAME"
ADDRESS="static:(tcp://$IP:61616)"

echo "Using etcd url $URL"
echo "Setting $URL to to $ADDRESS"
curl -XPUT $URI_URL -d value="$ADDRESS"
curl -XPUT $IP_URL -d value="$IP"

sleep 2

echo "SITEA: `curl http://172.17.42.1:4001/v2/keys/ip/site-a` SITEB: `curl http://172.17.42.1:4001/v2/keys/ip/site-b` SITEC: `curl http://172.17.42.1:4001/v2/keys/ip/site-c`"

SITEA_NETWORK_CONNECTOR=`curl http://172.17.42.1:4001/v2/keys/uri/site-a | perl -ne 'if (/.*"value":"([()\w\d.:\/]+)".*/) {print $1}'`
SITEB_NETWORK_CONNECTOR=`curl http://172.17.42.1:4001/v2/keys/uri/site-b | perl -ne 'if (/.*"value":"([()\w\d.:\/]+)".*/) {print $1}'`
SITEC_NETWORK_CONNECTOR=`curl http://172.17.42.1:4001/v2/keys/uri/site-c | perl -ne 'if (/.*"value":"([()\w\d.:\/]+)".*/) {print $1}'`

echo "Setting SITEA_NETWORK_CONNECTOR to $SITEA_NETWORK_CONNECTOR"
echo "Setting SITEB_NETWORK_CONNECTOR to $SITEB_NETWORK_CONNECTOR"
echo "Setting SITEC_NETWORK_CONNECTOR to $SITEC_NETWORK_CONNECTOR"

MY_ENV=`env`

echo "My ENV: $MY_ENV"

export SITEA_NETWORK_CONNECTOR
export SITEB_NETWORK_CONNECTOR
export SITEC_NETWORK_CONNECTOR

java -Xms1G -Xmx1G -Djava.util.logging.config.file=logging.properties -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote -Djava.io.tmpdir=/tmp -Dactivemq.classpath=apache-activemq-5.5.1/conf -Dactivemq.home=apache-activemq-5.5.1 -Dactivemq.base=apache-activemq-5.5.1 -Dactivemq.conf=apache-activemq-5.5.1/conf -Dactivemq.data=apache-activemq-5.5.1/data -jar apache-activemq-5.5.1/bin/run.jar start
