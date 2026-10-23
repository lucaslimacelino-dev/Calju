# Imagem do backend para produção. Etapa 1 compila com o JDK; a imagem final só tem o JRE e o jar.
# Uso: docker build -t calju-backend backend
FROM eclipse-temurin:17-jdk-noble AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Dependências numa camada própria: mudar o código não baixa tudo de novo.
RUN ./mvnw -B -q dependency:go-offline
COPY src src
# Os testes precisam de Docker (Testcontainers) e rodam fora da imagem: ./mvnw test
RUN ./mvnw -B -q -DskipTests package && cp target/calju-backend-*.jar /src/app.jar

FROM eclipse-temurin:17-jre-noble
RUN useradd --system --uid 10001 --no-create-home calju
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER calju
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
