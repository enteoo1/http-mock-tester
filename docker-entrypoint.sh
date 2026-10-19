#!/bin/sh
set -e
SSL_ENABLED="${APP_SSL_ENABLED:-true}"
KS="${APP_SSL_KEYSTORE:-/data/certs/keystore.p12}"
KS_PASS="${APP_SSL_KEYSTORE_PASSWORD:-changeit}"
CN="${APP_SSL_CN:-localhost}"

if [ "$SSL_ENABLED" = "true" ] && [ ! -f "$KS" ]; then
  echo "No keystore found at $KS - generating a self-signed certificate for CN=$CN"
  mkdir -p "$(dirname "$KS")"
  keytool -genkeypair -alias mocktester -keyalg RSA -keysize 2048 -storetype PKCS12 \
    -keystore "$KS" -storepass "$KS_PASS" -validity 3650 \
    -dname "CN=$CN" -ext "SAN=dns:$CN,dns:localhost,ip:127.0.0.1"
fi

# Without SSL the main port is plain HTTP
if [ "$SSL_ENABLED" != "true" ] && [ -z "$APP_PORT" ]; then
  export APP_PORT=8080
fi

exec java $JAVA_OPTS -jar /app/app.jar
