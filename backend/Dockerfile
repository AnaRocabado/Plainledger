# syntax=docker/dockerfile:1

# ---- Build: compile and package. Tests run in CI (they need Docker for Testcontainers).
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q package -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true \
 && java -Djarmode=tools -jar target/plainledger-backend-*.jar extract --layers --launcher --destination target/extracted

# ---- Runtime: JRE only, non-root. Layers are ordered from least to most frequently changed,
# so a code change rebuilds only the small application layer.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S plainledger && adduser -S plainledger -G plainledger
WORKDIR /app
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./
USER plainledger
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
