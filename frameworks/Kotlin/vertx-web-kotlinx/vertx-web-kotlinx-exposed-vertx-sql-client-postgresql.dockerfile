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

# Create a wrapper script to handle profiling
RUN echo '#!/bin/bash\n\
set -e\n\
\n\
# Start the application in background\n\
with-db/exposed-vertx-sql-client/build/install/exposed-vertx-sql-client/bin/exposed-vertx-sql-client &\n\
APP_PID=$!\n\
\n\
# Trap signals to ensure profiler stops cleanly\n\
cleanup() {\n\
    echo "Stopping application and profiler..." >&2\n\
    # Stop the profiler to flush results\n\
    /opt/async-profiler-4.3-linux-x64/profiler.sh stop -o html -f /tmp/profile.html $APP_PID 2>/dev/null || true\n\
    # Give it a moment to write\n\
    sleep 2\n\
    # Kill the application if still running\n\
    kill $APP_PID 2>/dev/null || true\n\
    wait $APP_PID 2>/dev/null || true\n\
    # Output the profiling results\n\
    echo "===PROFILING_RESULTS_START==="\n\
    cat /tmp/profile.html 2>/dev/null || echo "No profiling results found"\n\
    echo "===PROFILING_RESULTS_END==="\n\
}\n\
\n\
trap cleanup EXIT TERM INT\n\
\n\
# Wait for the application\n\
wait $APP_PID\n\
' > /start-with-profiling.sh && chmod +x /start-with-profiling.sh

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
    /start-with-profiling.sh
