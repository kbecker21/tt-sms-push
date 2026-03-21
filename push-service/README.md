# TTM Push Service

Push-Benachrichtigungen für Tischtennis-Turniere via W3C Web Push API (RFC 8030 + VAPID RFC 8292).

Kein Firebase, kein Spring Boot — reines Java mit JDK HttpServer.

## Schnellstart

### 1. Dependencies laden und kompilieren

```bash
cd push-service
ant resolve
ant compile
```

### 2. VAPID Keys generieren

```bash
ant generate-vapid-keys
```

Die generierten Keys in `conf/push-service.properties` eintragen:
```properties
vapid.public.key=BASE64URL_PUBLIC_KEY
vapid.private.key=BASE64URL_PRIVATE_KEY
```

Den **Public Key** zusätzlich in `web/push/app.js` eintragen:
```javascript
const VAPID_PUBLIC_KEY = 'BASE64URL_PUBLIC_KEY';
```

### 3. Starten

```bash
ant dist
cd dist
start.bat    # Windows
./start.sh   # Linux
```

Der Server ist erreichbar unter: `http://localhost:8080/push/`

## Lokaler Test

PWA öffnen:
```
http://localhost:8080/push/?player=TEST01
```

Push senden per curl:
```bash
curl -X POST http://localhost:8080/api/push/send \
  -H "Authorization: Bearer changeme" \
  -H "Content-Type: application/json" \
  -d '{"playerId": "TEST01", "message": "Ergebnis: Mueller 3:1 Schmidt"}'
```

## Deployment (ttm.co.at)

### Reverse Proxy (Apache/Nginx)

Nginx Beispiel:
```nginx
location /push/ {
    proxy_pass http://localhost:8080/push/;
}
location /api/push/ {
    proxy_pass http://localhost:8080/api/push/;
}
```

### QR-Code URL Format

```
https://www.ttm.co.at/push/?player={spielernummer}
```

Beispiel: `https://www.ttm.co.at/push/?player=2001`

## API-Dokumentation

### POST /api/push/register (kein Auth)

Registriert ein Browser-Gerät für Push-Benachrichtigungen.

```json
{
    "playerId": "2001",
    "subscription": {
        "endpoint": "https://fcm.googleapis.com/...",
        "keys": {
            "p256dh": "BASE64URL...",
            "auth": "BASE64URL..."
        }
    }
}
```

### POST /api/push/send (Bearer Token Auth)

Header: `Authorization: Bearer <api-key>`

```json
{
    "playerId": "2001",
    "message": "Ergebnis: Mueller 3:1 Schmidt"
}
```

Response:
```json
{
    "success": true,
    "sent": 1,
    "timestamp": "Mon 14:30 CET"
}
```

## Datenbank

### Entwicklung: H2 (Standard)

```properties
db.url=jdbc:h2:file:./data/pushdb
db.username=sa
db.password=
```

### Produktion: MS SQL Server

```properties
db.url=jdbc:sqlserver://SERVER:1433;databaseName=DBNAME;encrypt=true;trustServerCertificate=true
db.username=BENUTZER
db.password=PASSWORT
```

## Dependencies

| Library | Version | Zweck |
|---------|---------|-------|
| Gson | 2.11.0 | JSON |
| SLF4J API | 2.0.13 | Logging API |
| SLF4J Simple | 2.0.13 | Logging Implementierung |
| H2 | 2.2.224 | Datenbank (Entwicklung) |
| MSSQL JDBC | 12.2.0 | Datenbank (Produktion) |

Keine Firebase, kein Spring Boot, kein Bouncy Castle, kein Jetty/Tomcat.
