FROM activemq

RUN rm -f /apache-servicemix-5.5.1/conf/activemq.xml
COPY activemq.xml /apache-activemq-5.5.1/conf/
COPY start.sh /

EXPOSE 61616

CMD sh /start.sh
