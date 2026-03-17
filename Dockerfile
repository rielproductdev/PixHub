# ============================================================
# Dockerfile — Multi-stage build
# ============================================================
# Multi-stage = múltiplas etapas de build, cada uma com sua imagem base.
# Vantagem: a imagem final contém APENAS o necessário para rodar,
# sem ferramentas de compilação (Maven, JDK completo).
# Resultado: imagem ~300MB em vez de ~800MB.
# ============================================================

# ---- Etapa 1: BUILD ----
# Usa imagem com Maven + JDK para compilar o código.
# "AS build" nomeia esta etapa para referência posterior.
FROM maven:3.9-eclipse-temurin-17-alpine AS build

# Define o diretório de trabalho dentro do container.
# Todos os comandos seguintes executam a partir deste diretório.
WORKDIR /app

# Copia APENAS o pom.xml primeiro (antes do código-fonte).
# Por quê? Docker cacheia cada camada (layer). Se o pom.xml não mudou,
# o Docker reutiliza o cache do "mvn dependency:go-offline" (que é lento).
# Assim, só baixa dependências novamente quando o pom.xml muda.
COPY pom.xml .

# Baixa todas as dependências para o cache local do Maven.
# -B = batch mode (sem output interativo, ideal para CI/CD).
RUN mvn dependency:go-offline -B

# Agora copia o código-fonte. Se só o código mudou (não o pom.xml),
# as dependências já estão cacheadas na camada anterior.
COPY src ./src

# Compila e empacota o .jar, pulando testes.
# -DskipTests: testes são executados no CI/CD, não no build da imagem.
# O .jar gerado fica em target/pixhub-0.1.0.jar
RUN mvn package -DskipTests -B

# ---- Etapa 2: RUNTIME ----
# Usa imagem JRE (Java Runtime Environment) — sem compilador.
# "alpine" = distribuição Linux mínima (~5MB), reduz tamanho da imagem.
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia APENAS o .jar da etapa de build. Nada mais.
# O Maven, código-fonte e dependências de build ficam para trás.
COPY --from=build /app/target/*.jar app.jar

# Porta que a aplicação Spring Boot escuta (padrão 8080).
# EXPOSE é documentação — não publica a porta automaticamente.
# Quem publica é o "ports" no docker-compose.yml.
EXPOSE 8080

# Comando executado quando o container inicia.
# ENTRYPOINT (vs CMD): não pode ser sobrescrito facilmente,
# garantindo que o container sempre roda a aplicação Java.
ENTRYPOINT ["java", "-jar", "app.jar"]
