FROM gradle:8.10.2-jdk21

# Publish the dependencies to Maven local and run "copy_maven_local.sh" first.
COPY .m2/repository/org/jetbrains/kotlinx /root/.m2/repository/org/jetbrains/kotlinx
WORKDIR /vertx-web-kotlinx
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts
COPY gradle.properties gradle.properties
COPY src src
RUN gradle --no-daemon installDist

EXPOSE 8080

CMD export JAVA_OPTS=" \
    -server \
    -XX:+UseNUMA \
    -XX:+UseParallelGC \
    -Dvertx.disableMetrics=true \
    -Dvertx.disableH2c=true \
    -Dvertx.disableWebsockets=true \
    -Dvertx.flashPolicyHandler=false \
    -Dvertx.threadChecks=false \
    -Dvertx.disableContextTimings=true \
    -Dvertx.disableTCCL=true \
    -Dvertx.disableHttpHeadersValidation=true \
    -Dio.netty.buffer.checkBounds=false \
    -Dio.netty.buffer.checkAccessible=false \
    " && \
    build/install/vertx-web-kotlinx-benchmark/bin/vertx-web-kotlinx-benchmark true
