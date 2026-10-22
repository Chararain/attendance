#!/bin/bash
# Render injects the PORT environment variable. Tomcat listens on
# 8080 by default, so patch server.xml before startup when PORT is set.
if [ -n "$PORT" ] && [ "$PORT" != "8080" ]; then
  sed -i "s/port=\"8080\"/port=\"$PORT\"/" "$CATALINA_HOME/conf/server.xml"
fi
exec "$CATALINA_HOME/bin/catalina.sh" run
