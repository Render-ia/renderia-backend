# Etapa 1: compilar el proyecto
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

# Etapa 2: ejecutar el proyecto
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Arranque más rápido en el plan gratis de Render (0.1 CPU):
# 1) se extrae el jar en capas y 2) se hace un arranque de entrenamiento que guarda
#    las clases ya cargadas (CDS). Si el entrenamiento falla, la imagen igual funciona.
RUN java -Djarmode=tools -jar app.jar extract --destination extracted \
    && cd extracted \
    && (java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar app.jar || true)

WORKDIR /app/extracted
# Memoria ajustada a 512 MB y compilador rápido para que el arranque en frío tarde menos.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError -XX:SharedArchiveFile=app.jsa"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
