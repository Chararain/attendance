# ---------- stage 1: compile the servlet and build a deployable war ----------
FROM tomcat:9.0-jdk11-temurin AS build

WORKDIR /app
COPY web ./web
COPY src ./src

RUN mkdir -p web/WEB-INF/classes \
 && javac -encoding UTF-8 \
        -cp "$CATALINA_HOME/lib/servlet-api.jar" \
        -d web/WEB-INF/classes \
        src/java/attendance/*.java \
 && cd web && jar -cvf /app/ROOT.war .

# ---------- stage 2: runtime image ----------
# The war is copied as ROOT.war so the app is served from "/" and
# https://<service>.onrender.com/ opens index.html directly.
FROM tomcat:9.0-jdk11-temurin

RUN rm -rf "$CATALINA_HOME"/webapps/*

COPY --from=build /app/ROOT.war "$CATALINA_HOME"/webapps/ROOT.war

COPY entrypoint.sh /usr/local/tomcat/bin/entrypoint.sh
RUN chmod +x /usr/local/tomcat/bin/entrypoint.sh

EXPOSE 8080
CMD ["/usr/local/tomcat/bin/entrypoint.sh"]
