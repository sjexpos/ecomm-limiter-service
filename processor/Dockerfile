FROM amazoncorretto:21-al2-jdk
LABEL AUTHOR = 'Sergio Exposito'
LABEL EMAIL = 'sjexpos@gmail.com'

# ENV JAVA_XMS                <set initial Java heap size>
# ENV JAVA_XMX                <set maximum Java heap size>
# ENV PORT                    <port to run server>
# ENV MANAGEMENT_PORT
# ENV MONITORING_URL
# ENV REDIS_HOST           <redis server host name>
# ENV REDIS_PORT           <redis server port>
# ENV KAFKA_SERVERS            <kafka servers host name and port>
# ENV KAFKA_SECURITY_PROTOCOL
# ENV KAFKA_SASL_MECHANISM
# ENV KAFKA_SASL_JAAS_CONFIG
# ENV KAFKA_EXTRAS
# ENV TRACING_URL             <url where tracing server is running>
# ENV RATE_LIMIT              <>

ADD target/*.jar /opt/limiter-processor.jar

RUN bash -c 'touch /opt/limiter-processor.jar'

RUN echo "#!/usr/bin/env bash" > /opt/entrypoint.sh && \
    echo "" >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "echo \"JAVA_XMS: \$JAVA_XMS \" " >> /opt/entrypoint.sh && \
    echo "echo \"JAVA_XMX: \$JAVA_XMX \" " >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "echo \"PORT: \$PORT \" " >> /opt/entrypoint.sh && \
    echo "echo \"MANAGEMENT_PORT: \$MANAGEMENT_PORT \" " >> /opt/entrypoint.sh && \
    echo "echo \"MONITORING_URL: \$MONITORING_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"REDIS_HOST: \$REDIS_HOST \" " >> /opt/entrypoint.sh && \
    echo "echo \"REDIS_PORT: \$REDIS_PORT \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_SERVERS: \$KAFKA_SERVERS \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_SECURITY_PROTOCOL: \$KAFKA_SECURITY_PROTOCOL \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_SASL_MECHANISM: \$KAFKA_SASL_MECHANISM \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_SASL_JAAS_CONFIG: \$KAFKA_SASL_JAAS_CONFIG \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_EXTRAS: \$KAFKA_EXTRAS \" " >> /opt/entrypoint.sh && \
    echo "echo \"TRACING_URL: \$TRACING_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"RATE_LIMIT: \$RATE_LIMIT \" " >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "" >> /opt/entrypoint.sh && \
    echo "echo \"singleServerConfig:\" > /opt/redisson.yaml " >> /opt/entrypoint.sh && \
    echo "echo \"  address: redis://\$REDIS_HOST:\$REDIS_PORT\" >> /opt/redisson.yaml " >> /opt/entrypoint.sh && \
    echo "echo \"  connectTimeout: 5000\" >> /opt/redisson.yaml " >> /opt/entrypoint.sh && \
    echo "echo \"  timeout: 1000\" >> /opt/redisson.yaml " >> /opt/entrypoint.sh && \
    echo "" >> /opt/entrypoint.sh && \
    echo "java -Xms\$JAVA_XMS -Xmx\$JAVA_XMX \
        -Dserver.port=\$PORT \
        -Dmanagement.server.port=\$MANAGEMENT_PORT \
        -Dspring.boot.admin.client.url=\$MONITORING_URL \
        -Dspring.redis.redisson.file=file:/opt/redisson.yaml \
        -Dspring.kafka.bootstrap-servers=\$KAFKA_SERVERS \
        -Dspring.kafka.properties.security.protocol=\$KAFKA_SECURITY_PROTOCOL \
        -Dspring.kafka.properties.sasl.mechanism=\$KAFKA_SASL_MECHANISM \
        -Dspring.kafka.properties.sasl.jaas.config=\"\$KAFKA_SASL_JAAS_CONFIG\" \
        \$KAFKA_EXTRAS \
        -Decomm.service.tracing.url=\$TRACING_URL \
        -Decomm.service.limiter.rate-limit=\$RATE_LIMIT \
        -jar /opt/limiter-processor.jar" >> /opt/entrypoint.sh

RUN chmod 755 /opt/entrypoint.sh

EXPOSE ${PORT}

ENTRYPOINT [ "/opt/entrypoint.sh" ]

