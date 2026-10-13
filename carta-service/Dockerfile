# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /app

# 1. Solo los pom.xml: el pom padre declara los cinco módulos y Maven
#    necesita encontrarlos todos, aunque compilemos solo uno.
COPY pom.xml .
COPY usuarios-service/pom.xml usuarios-service/
COPY carta-service/pom.xml    carta-service/
COPY clientes-service/pom.xml clientes-service/
COPY salon-service/pom.xml    salon-service/
COPY api-gateway/pom.xml      api-gateway/

# 2. Descargar dependencias. Docker guarda esta capa en caché: mientras los
#    pom.xml no cambien, no se vuelve a descargar nada.
RUN mvn -B -q -pl carta-service -am dependency:go-offline

# 3. Ahora sí, el código del módulo, y se empaqueta.
COPY carta-service/src carta-service/src
RUN mvn -B -q -pl carta-service -am package -DskipTests

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilacion /app/carta-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
