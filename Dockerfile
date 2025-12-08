FROM gradle:8.14.3-jdk21-alpine as base
LABEL authors="boonsboos"

WORKDIR /
COPY . .

RUN gradle build

FROM eclipse-temurin:25-jre-alpine-3.22 as final
COPY --from=base /build/libs/kdn-all.jar /
COPY --from=base /src/main/resources/application.yaml /

ENTRYPOINT ["java", "-jar", "kdn-all.jar"]