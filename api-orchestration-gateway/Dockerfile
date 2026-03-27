FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY common/pom.xml common/
COPY gateway-service/pom.xml gateway-service/
RUN mvn -q -B dependency:go-offline -pl common,gateway-service -am || true
COPY common/src common/src
COPY gateway-service/src gateway-service/src
RUN mvn -q -B -pl gateway-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r gateway && useradd -r -g gateway gateway
COPY --from=builder /build/gateway-service/target/gateway-service-*.jar app.jar
USER gateway
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
