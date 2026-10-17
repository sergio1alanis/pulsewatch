FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN sh ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app
ENV PULSEWATCH_REQUIRE_API_KEY=true
RUN groupadd --system pulsewatch \
    && useradd --system --uid 10001 --gid pulsewatch --no-create-home pulsewatch
COPY --from=build --chown=10001:10001 /workspace/target/pulsewatch-0.0.1-SNAPSHOT.jar /app/app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
