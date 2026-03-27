FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY common/pom.xml common/
COPY gateway-service/pom.xml gateway-service/
RUN apk add --no-cache maven
RUN mvn -q -B dependency:go-offline -pl common,gateway-service -am || true
COPY common/src common/src
COPY gateway-service/src gateway-service/src
RUN mvn -q -B -pl gateway-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S gateway && adduser -S gateway -G gateway
COPY --from=builder /build/gateway-service/target/gateway-service-*.jar app.jar
USER gateway
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
