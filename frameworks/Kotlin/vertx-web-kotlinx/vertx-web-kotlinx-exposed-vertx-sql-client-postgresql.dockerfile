FROM gradle:9.3.1-jdk25

WORKDIR /vertx-web-kotlinx


# copy the Maven local dependencies into the container for snapshot dependencies
# First publish with `publishToMavenLocal` and copy the Maven local dependencies into this directory with `cp -r ~/.m2 ./`.
COPY .m2/repository/com/huanshankeji/exposed-vertx-sql-client-core/0.8.0-SNAPSHOT /root/.m2/repository/com/huanshankeji/exposed-vertx-sql-client-core/0.8.0-SNAPSHOT
COPY .m2/repository/com/huanshankeji/exposed-vertx-sql-client-postgresql/0.8.0-SNAPSHOT /root/.m2/repository/com/huanshankeji/exposed-vertx-sql-client-postgresql/0.8.0-SNAPSHOT

COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY buildSrc buildSrc
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY gradle.properties gradle.properties

# make empty directories for subprojects that do not need to be copied for Gradle
RUN mkdir -p common without-db/default with-db/common with-db/default with-db/r2dbc-common with-db/r2dbc with-db/exposed-common with-db/exposed-r2dbc with-db/exposed-vertx-sql-client

COPY common/build.gradle.kts common/build.gradle.kts
COPY common/src common/src

COPY with-db/common/build.gradle.kts with-db/common/build.gradle.kts
COPY with-db/common/src with-db/common/src

COPY with-db/exposed-common/build.gradle.kts with-db/exposed-common/build.gradle.kts
COPY with-db/exposed-common/src with-db/exposed-common/src

COPY with-db/exposed-vertx-sql-client/build.gradle.kts with-db/exposed-vertx-sql-client/build.gradle.kts
COPY with-db/exposed-vertx-sql-client/src with-db/exposed-vertx-sql-client/src


RUN gradle --no-daemon with-db:exposed-vertx-sql-client:installDist

# Install async-profiler for profiling (optional but recommended)
RUN apt-get update && apt-get install -y wget && \
    wget -q https://github.com/async-profiler/async-profiler/releases/download/v4.3/async-profiler-4.3-linux-x64.tar.gz && \
    tar -xzf async-profiler-4.3-linux-x64.tar.gz -C /opt && \
    rm async-profiler-4.3-linux-x64.tar.gz

EXPOSE 8080

# TRANSACTION_PROVIDER environment variable can be set to "jdbc" (default) or "database"
ENV TRANSACTION_PROVIDER=jdbc

# Copy the profiling wrapper script.
# This uses a timed dump approach: a background timer stops the profiler and outputs
# flame graph HTML to stdout DURING the benchmark, so it's captured in Docker logs
# before the container is removed (the trap-based approach doesn't work because TFB
# kills the container before the cleanup handler can execute).
COPY start-with-profiling.sh /start-with-profiling.sh
RUN chmod +x /start-with-profiling.sh

CMD export JAVA_OPTS=" \
    -agentpath:/opt/async-profiler-4.3-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=1000000 \
    --enable-native-access=ALL-UNNAMED \
    --sun-misc-unsafe-memory-access=allow \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    -server \
    -XX:+UseNUMA \
    -XX:+UseParallelGC \
    -XX:+UnlockDiagnosticVMOptions \
    -XX:+DebugNonSafepoints \
    -Djava.lang.Integer.IntegerCache.high=10000 \
    -Dvertx.disableMetrics=true \
    -Dvertx.disableWebsockets=true \
    -Dvertx.disableContextTimings=true \
    -Dvertx.disableHttpHeadersValidation=true \
    -Dvertx.cacheImmutableHttpResponseHeaders=true \
    -Dvertx.internCommonHttpRequestHeadersToLowerCase=true \
    -Dio.netty.noUnsafe=false \
    -Dio.netty.buffer.checkBounds=false \
    -Dio.netty.buffer.checkAccessible=false \
    -Dio.netty.iouring.ringSize=16384 \
    " && \
    exec /start-with-profiling.sh
