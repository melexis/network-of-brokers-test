#!/bin/dash

SITEA_NETWORK_CONNECTOR="static:($NETWORKOFBROKERSTEST_SITEA_1_PORT_61616_TCP)"
SITEB_NETWORK_CONNECTOR="static:($NETWORKOFBROKERSTEST_SITEB_1_PORT_61616_TCP)"
SITEC_NETWORK_CONNECTOR="static:($NETWORKOFBROKERSTEST_SITEC_1_PORT_61616_TCP)"

java -Xms1G -Xmx1G -Djava.util.logging.config.file=logging.properties -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote -Djava.io.tmpdir=/tmp -Dactivemq.classpath=apache-activemq-5.5.1/conf -Dactivemq.home=apache-activemq-5.5.1 -Dactivemq.base=apache-activemq-5.5.1 -Dactivemq.conf=apache-activemq-5.5.1/conf -Dactivemq.data=apache-activemq-5.5.1/data -jar apache-activemq-5.5.1/bin/run.jar start
