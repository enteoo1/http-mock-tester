# Mock Tester

Spring Boot 3 / Java 21 / Maven / SQLite app to simulate HTTP endpoints (Solace RDP, Salesforce, Datadog webhooks...).

## Build
Create Jar
```
mvn clean compile package
```
Create Docker Image
```
 docker build . -t enteoo1/http-mock-tester:latest -f Dockerfile
 docker tag enteoo1/http-mock-tester:latest enteoo1/http-mock-tester:v1.0.0
 docker push --all-tags enteoo1/http-mock-tester

```

## Run with Docker
```
docker compose up --build
```
* HTTPS: https://localhost:8443  (self-signed certificate generated on first start into ./data/certs)
* HTTP:  http://localhost:8080   (only if `APP_HTTP_ENABLED=true`)
* First login: **admin / admin** - you are forced to change the password.
* Data (SQLite DB + certificates) lives in `./data`.

## Run locally (no Docker)
```
APP_SSL_ENABLED=false APP_PORT=8080 mvn spring-boot:run
```

## Environment variables
| Variable | Default | Meaning |
|---|---|---|
| APP_SSL_ENABLED | true | HTTPS on the main port. `false` = plain HTTP only |
| APP_PORT | 8443 (8080 in Docker when SSL is disabled) | main port |
| APP_HTTP_ENABLED | false | extra plain HTTP connector next to HTTPS |
| APP_HTTP_PORT | 8080 | port of the extra HTTP connector |
| APP_SSL_KEYSTORE | /data/certs/keystore.p12 | PKCS12 keystore (mount your own to use a real certificate) |
| APP_SSL_KEYSTORE_PASSWORD | changeit | keystore password |
| APP_SSL_CN | localhost | CN/SAN of the generated self-signed certificate |
| APP_DB_PATH | ./data/mocktester.db (/data/mocktester.db in Docker) | SQLite file |

## Mock URL
`/mock/{username}/{context}/{scenario}` (any extra path after the scenario is accepted too).

Example - scenario "A": POST + JSON body + Basic auth, 200, empty body:
```
curl -k -u user:pass -H 'Content-Type: application/json' -d '{"a":1}' \
  https://localhost:8443/mock/alice/testing/A
```
Scenario "B": any method, always returns 500.

## Behaviour
| Situation | Response | Logged outcome |
|---|---|---|
| user/context/scenario unknown | 404 | NOT_FOUND |
| scenario disabled | 503 | DISABLED |
| method not accepted | 405 (+Allow header) | METHOD_NOT_ALLOWED |
| credentials/header missing or empty | 401 | UNAUTHORIZED |
| body present but FORBIDDEN, absent but REQUIRED, or invalid JSON (when enabled) | 400 | BAD_REQUEST |
| everything ok | configured status / headers / body after the configured delay | MATCHED |

Authentication is only checked for presence (Basic: user and password non-empty; Bearer/OAuth2: non-empty token; API key: header present and non-empty). Values are never verified.

All requests (including rejected ones) are stored in `request_log` with UTC timestamps; the UI renders them in each user's timezone (Account page).
Logs older than the retention period (default 60 days, admin-configurable) are purged nightly at 03:30 UTC.
