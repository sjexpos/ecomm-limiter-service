FROM amazoncorretto:21-al2-jdk
LABEL AUTHOR = 'Sergio Exposito'
LABEL EMAIL = 'sjexpos@gmail.com'

# ENV JAVA_XMS                  <set initial Java heap size>
# ENV JAVA_XMX                  <set maximum Java heap size>
# ENV PORT                      <port to run server>
# ENV MANAGEMENT_PORT
# ENV MONITORING_URL
# ENV KAFKA_SERVERS            <kafka servers host name and port>
# ENV KAFKA_SECURITY_PROTOCOL
# ENV KAFKA_SASL_MECHANISM
# ENV KAFKA_SASL_JAAS_CONFIG
# ENV KAFKA_EXTRAS
# ENV TRACING_URL
# ENV PROCESSOR_SERVICE_BASEURI 
# ENV READER_QUEUE_SIZE         <>
# ENV WRITER_THREADS            <>

ADD target/*.jar /opt/limiter-kafka-mps.jar

RUN bash -c 'touch /opt/limiter-kafka-mps.jar'

RUN echo "#!/usr/bin/env bash" > /opt/entrypoint.sh && \
    echo "" >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "echo \"JAVA_XMS: \$JAVA_XMS \" " >> /opt/entrypoint.sh && \
    echo "echo \"JAVA_XMX: \$JAVA_XMX \" " >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "echo \"PORT: \$PORT \" " >> /opt/entrypoint.sh && \
    echo "echo \"MANAGEMENT_PORT: \$MANAGEMENT_PORT \" " >> /opt/entrypoint.sh && \
    echo "echo \"MONITORING_URL: \$MONITORING_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_SERVERS: \$KAFKA_SERVERS \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_SECURITY_PROTOCOL: \$KAFKA_SECURITY_PROTOCOL \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_SASL_MECHANISM: \$KAFKA_SASL_MECHANISM \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_SASL_JAAS_CONFIG: \$KAFKA_SASL_JAAS_CONFIG \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_EXTRAS: \$KAFKA_EXTRAS \" " >> /opt/entrypoint.sh && \
    echo "echo \"TRACING_URL: \$TRACING_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"PROCESSOR_SERVICE_BASEURI: \$PROCESSOR_SERVICE_BASEURI \" " >> /opt/entrypoint.sh && \
    echo "echo \"READER_QUEUE_SIZE: \$READER_QUEUE_SIZE \" " >> /opt/entrypoint.sh && \
    echo "echo \"WRITER_THREADS: \$WRITER_THREADS \" " >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "" >> /opt/entrypoint.sh && \
    echo "java -Xms\$JAVA_XMS -Xmx\$JAVA_XMX \
        -Dserver.port=\$PORT \
        -Dmanagement.server.port=\$MANAGEMENT_PORT \
        -Dspring.boot.admin.client.url=\$MONITORING_URL \
        -Dspring.kafka.bootstrap-servers=\$KAFKA_SERVERS \
        -Dspring.kafka.properties.security.protocol=\$KAFKA_SECURITY_PROTOCOL \
        -Dspring.kafka.properties.sasl.mechanism=\$KAFKA_SASL_MECHANISM \
        -Dspring.kafka.properties.sasl.jaas.config=\"\$KAFKA_SASL_JAAS_CONFIG\" \
        \$KAFKA_EXTRAS \
        -Decomm.service.tracing.url=\$TRACING_URL \
        -Decomm.service.limiter.processor.baseUri=\$PROCESSOR_SERVICE_BASEURI \
        -Decomm.service.limiter.mps.reader.queue-size=\$READER_QUEUE_SIZE \
        -Decomm.service.limiter.mps.writer.threads=\$WRITER_THREADS \
        -jar /opt/limiter-kafka-mps.jar" >> /opt/entrypoint.sh

RUN chmod 755 /opt/entrypoint.sh

EXPOSE ${PORT}

ENTRYPOINT [ "/opt/entrypoint.sh" ]

