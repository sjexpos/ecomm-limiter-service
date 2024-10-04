FROM amazoncorretto:21-al2-jdk

# ENV JAVA_XMS                <set initial Java heap size>
# ENV JAVA_XMX                <set maximum Java heap size>
# ENV PORT                    <port to run server>
# ENV ADMIN_SERVER_URL        <Spring Admin Server url>
# ENV KAFKA_SERVERS           <list of kafka servers>
# ENV TRACING_SERVER_URL      <url where tracing server is running>
# ENV REDIS_NODES             <>
# ENV TOPIC_BLACKEDLIST       <>
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
    echo "echo \"ADMIN_SERVER_URL: \$ADMIN_SERVER_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_SERVERS: \$KAFKA_SERVERS \" " >> /opt/entrypoint.sh && \
    echo "echo \"TRACING_SERVER_URL: \$TRACING_SERVER_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"REDIS_NODES: \$REDIS_NODES \" " >> /opt/entrypoint.sh && \
    echo "echo \"TOPIC_BLACKEDLIST: \$TOPIC_BLACKEDLIST \" " >> /opt/entrypoint.sh && \
    echo "echo \"RATE_LIMIT: \$RATE_LIMIT \" " >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "" >> /opt/entrypoint.sh && \
    echo "java -Xms\$JAVA_XMS -Xmx\$JAVA_XMX \
        -Dserver.port=\$PORT \
        -Dmanagement.server.port=\$PORT \
        -Dspring.boot.admin.client.url=\$ADMIN_SERVER_URL \
        -Dspring.kafka.bootstrap-servers=\$KAFKA_SERVERS \
        -Decomm.service.tracing.url=\$TRACING_SERVER_URL \
        -Dspring.data.redis.cluster.nodes=\$REDIS_NODES \
        -Decomm.service.limiter.topics.blacklisted-users=\$TOPIC_BLACKEDLIST \
        -Decomm.service.limiter.rate-limit=\$RATE_LIMIT \
        -jar /opt/limiter-processor.jar" >> /opt/entrypoint.sh

RUN chmod 755 /opt/entrypoint.sh

EXPOSE ${PORT}

ENTRYPOINT [ "/opt/entrypoint.sh" ]

