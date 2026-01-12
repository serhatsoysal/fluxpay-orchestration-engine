FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY fluxpay-common/pom.xml fluxpay-common/
COPY fluxpay-security/pom.xml fluxpay-security/
COPY fluxpay-tenant/pom.xml fluxpay-tenant/
COPY fluxpay-product/pom.xml fluxpay-product/
COPY fluxpay-subscription/pom.xml fluxpay-subscription/
COPY fluxpay-billing/pom.xml fluxpay-billing/
COPY fluxpay-notification/pom.xml fluxpay-notification/
COPY fluxpay-api/pom.xml fluxpay-api/
COPY fluxpay-coverage-report/pom.xml fluxpay-coverage-report/
RUN mvn dependency:go-offline -B
COPY fluxpay-common/src fluxpay-common/src
COPY fluxpay-security/src fluxpay-security/src
COPY fluxpay-tenant/src fluxpay-tenant/src
COPY fluxpay-product/src fluxpay-product/src
COPY fluxpay-subscription/src fluxpay-subscription/src
COPY fluxpay-billing/src fluxpay-billing/src
COPY fluxpay-api/src fluxpay-api/src
RUN mvn clean package -DskipTests -pl fluxpay-api -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/fluxpay-api/target/*.jar app.jar
EXPOSE 10000
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:10000/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=10000"]

