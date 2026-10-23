# Dockerfile: the recipe for packaging the whole app into a "container image".
#
# WHAT'S A CONTAINER?
# A sealed box holding the app plus everything it needs to run: a small Linux
# system, Java, and our built code. Any host that runs containers (Render,
# Railway, Fly.io, Google Cloud Run, your own server...) can run the same box
# the same way. That's what makes it easy to change hosts later: none of this
# file is specific to Render.
#
# Render builds this file on every push to GitHub, and the "Build and test"
# workflow (.github/workflows/ci.yml) builds and tries it out too.
#
# MULTI-STAGE BUILD
# Building the app needs big tools (Node.js, Maven, a full Java Development
# Kit) that running it doesn't. So we build in throwaway "stages" and copy
# only the finished result into a small final image:
#   1. frontend: Node.js builds the React app into plain HTML/CSS/JS
#   2. backend:  Maven builds the Spring Boot .jar, with the React app inside
#   3. runtime:  just Java (JRE) + the .jar; this is what actually runs online
#
# Each line is a step. Docker remembers ("caches") steps whose inputs haven't
# changed, which is why package files are copied before the rest of the code:
# when only code changes, the slow "download all the libraries" steps are reused.

# ---------- Stage 1: build the React app ----------
FROM node:24-slim AS frontend
WORKDIR /app/frontend
# Install the packages exactly as listed in package-lock.json.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
# Then the rest of the React code, and build it. vite.config.ts writes the
# result to ../src/main/resources/static, i.e. /app/src/main/resources/static.
COPY frontend/ ./
RUN npm run build

# ---------- Stage 2: build the Spring Boot .jar ----------
# The same Maven (3.9.11) and Java (17) versions we use on our computers.
FROM maven:3.9.11-eclipse-temurin-17 AS backend
WORKDIR /app
# Download the libraries listed in pom.xml first (cached between builds).
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
# Our Java code, plus the React app built in stage 1.
COPY src ./src
COPY --from=frontend /app/src/main/resources/static ./src/main/resources/static
# Package the .jar. The tests are skipped here because the "Build and test"
# workflow already runs them all on GitHub before anything is deployed.
RUN mvn -B -q -DskipTests package && cp target/orthodoxapp-*.jar app.jar
# Unpack the .jar into app.jar + lib/ (the libraries as separate files).
# Java starts a little faster this way, and it's needed for the speed-up in stage 3.
RUN java -Djarmode=tools -jar app.jar extract --destination /extracted

# ---------- Stage 3: the image that runs online ----------
# Only the Java Runtime Environment (JRE), not the full development kit.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /extracted ./

# Java settings, tuned for a small server (the free plan has 512 MB of memory
# and a tenth of a processor):
#   MaxRAMPercentage=60     use at most 60% of the memory for Java objects,
#                           leaving the rest for Java itself
#   UseSerialGC             the simplest memory cleaner: least overhead on one small CPU
#   Xss512k                 smaller memory per thread
#   TieredStopAtLevel=1     compile code quickly rather than perfectly: faster startup
#   ExitOnOutOfMemoryError  if memory ever runs out, stop so the host restarts the app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"

# Faster startup with "Class Data Sharing": start the app once now, while
# building, and save the Java classes it loaded into app.jsa. Real starts
# then load them from that file instead of from scratch, which matters because
# free apps on Render go to sleep and start up again when someone visits.
# spring.context.exit=onRefresh stops it as soon as it's set up, and the
# in-memory H2 database stands in because there's no real database here.
RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh \
         -Dspring.datasource.url=jdbc:h2:mem:training -jar app.jar

# Switch on application-prod.properties (PostgreSQL, https, smaller limits).
ENV SPRING_PROFILES_ACTIVE=prod

# Don't run as the all-powerful "root" user: if anyone ever broke into the
# app, they'd be stuck as this limited user.
RUN useradd --system --no-create-home orthodoxapp
USER orthodoxapp

# The app listens on $PORT (Render sets it) or 8080.
EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-jar", "app.jar"]
