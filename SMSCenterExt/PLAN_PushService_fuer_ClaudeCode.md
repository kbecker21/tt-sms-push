# SMSCenter Push-Erweiterung — Implementierungsplan für Claude Code

## Kontext

Das SMSCenter-Projekt (https://github.com/kbecker21/SMSCenter) ist eine Java-Desktop-Anwendung
für Tischtennis-Turniere, die SMS-Benachrichtigungen an Spieler verschickt.
Es soll um Push-Benachrichtigungen erweitert werden.

Ein erster Versuch durch Claude Code (Commit `4a1c143`) hat eine Firebase-basierte Lösung
erzeugt, die aber zu komplex ist. Dieser Plan beschreibt die gewünschte **Firebase-freie** Lösung.

---

## Rahmenbedingungen (bereits geklärt)

| Frage | Antwort |
|-------|---------|
| Build-System PushServiceProvider | **Ivy/Ant** (wie das SMSCenter selbst, kein Maven) |
| Java-Version auf dem Server | **Java 21+** |
| HTTPS auf ttm.co.at | **Ja, vorhanden** |
| Datenbank Entwicklung | **H2** |
| Datenbank Produktion | **MS SQL Server** (wie bei allen Turnier-Daten) |
| Firebase | **Nein — komplett ohne Firebase** |
| Spring Boot | **Nein — reines Java mit JDK HttpServer** |
| Push-Technologie | **W3C Web Push API (RFC 8030) mit VAPID (RFC 8292)** |

---

## Architekturüberblick

```
SMSCenter (Desktop-App)              Push-Service (eigener Prozess)         Browser (Spieler)
┌──────────────────────┐  HTTP POST  ┌──────────────────────────┐  Web Push  ┌──────────────┐
│ PushHTTPGateway.java │────────────>│ POST /api/push/send      │──────────>│ Service       │
│ (OkHttp + Gson)      │             │   (Bearer-Token Auth)    │           │ Worker (sw.js)│
└──────────────────────┘             │                          │           │               │
                                     │ POST /api/push/register  │<──────────│ PWA (app.js)  │
                                     │   (kein Auth, von PWA)   │           └───────────────┘
                                     │                          │
                                     │ GET /push/* (statische   │
                                     │   PWA-Dateien)           │
                                     │                          │
                                     │ H2 / MS SQL Server       │
                                     └──────────────────────────┘
```

Drei Teile:
1. **SMSCenter-Erweiterung** — Neue/geänderte Dateien im bestehenden SMSCenter-Projekt
2. **Push-Service** — Eigenständiges Java-Projekt mit Ivy/Ant im Unterverzeichnis `push-service/`
3. **PWA** — Statische Web-Dateien (HTML/JS/CSS) im Push-Service unter `web/push/`

---

## Teil 1: SMSCenter-Erweiterung (bestehende Dateien anpassen)

### 1.1 Bug Fix: PushHTTPGateway.java

**Datei:** `src/smscenter/smsserver/gateways/PushHTTPGateway.java`
**Problem:** Zeile 33 ruft `super(id, "Push Notification Gateway")` auf, aber `org.smslib.AGateway`
hat in SMSLib v3 nur einen 1-Parameter-Konstruktor: `AGateway(String id)`.
**Fix:** Ändern auf `super(id);`

### 1.2 KRITISCH: Recipient-Mapping (Telefonnummer → Spielernummer)

**Das Kernproblem:**
- Die `Database.sendMessage()` Methode (Zeile 425 in `src/smscenter/database/Database.java`)
  nimmt eine Spielernummer (z.B. "2001"), schlägt die Telefonnummer in `smscenter_phones` nach,
  und schreibt die **Telefonnummer** (z.B. "+4369912345678") als `recipient` in `smsserver_out`.
- Der `PushHTTPGateway.sendMessage()` liest `msg.getRecipient()` = die Telefonnummer,
  und schickt sie als `playerId` an den Push-Service.
- Der Spieler hat sich aber in der PWA mit seiner **Spielernummer** (2001) registriert,
  NICHT mit seiner Telefonnummer.
- → Der Push-Service findet keinen Treffer, die Nachricht geht verloren.

**Empfohlene Lösung: Rückwärts-Suche im PushHTTPGateway**

Der PushHTTPGateway soll vor dem Senden eine Rückwärts-Suche machen:
Telefonnummer → `smscenter_phones.phone` → `smscenter_phones.plNr` → playerId

Dazu braucht der PushHTTPGateway Zugriff auf die SMSCenter-Datenbank.
Der einfachste Weg: Die bestehende JDBC-Connection aus dem SMSServer-Kontext nutzen.

Schau dir an, wie andere Gateways (z.B. `BulkSmsHttp.java`) auf Properties und Server zugreifen.
Der Wrapper `PushGateway.java` (extends `smscenter.smsserver.gateways.AGateway`) hat Zugriff
auf `getProperties()` und `getServer()`. Über den Server kommt man an die DB-Interfaces.

Alternativ: Die Properties enthalten die DB-Connection-Daten. Der PushHTTPGateway könnte
eine eigene JDBC-Connection zur selben Datenbank aufbauen.

**SQL für die Rückwärts-Suche:**
```sql
SELECT DISTINCT plNr FROM smscenter_phones WHERE phone LIKE ?
```
(mit der Telefonnummer als Parameter, ggf. mit/ohne führendes "+")

Wenn kein Treffer → Warnung loggen und die Telefonnummer als Fallback verwenden.
Wenn Treffer → Die plNr als String (z.B. "2001") als playerId verwenden.

### 1.3 Bereits vorhandene Änderungen (aus Commit 4a1c143) prüfen und beibehalten

Diese Dateien aus dem bestehenden Commit sind **korrekt und können bleiben** (nach dem Bug-Fix):

- `src/smscenter/smsserver/gateways/PushGateway.java` — Wrapper, liest serviceUrl + apiKey aus Properties
- `src/smscenter/smsserver/gateways/PushHTTPGateway.java` — HTTP-Gateway (nach Bug-Fix + Recipient-Mapping)
- `src/smscenter/gui/settings/PushGatewaySettings.java` — Einstellungen
- `src/smscenter/gui/settings/PushGatewaySettingsPanel.java` — Swing-UI Panel
- `src/smscenter/gui/settings/GatewaySettings.java` — PushGateway zum Enum hinzugefügt
- `src/smscenter/gui/settings/Settings.java` — PushGateway in readGateways()/writeGateways()
- `src/smscenter/gui/resources/SMSCenter.properties` — i18n Keys
- `ivy.xml` — OkHttp 4.12.0 + Gson 2.11.0

### 1.4 Zu ENTFERNEN aus dem Commit

Das gesamte alte `push-service/` Verzeichnis (Maven/Spring Boot/Firebase) wird ersetzt durch
die neue Version (siehe Teil 2).

---

## Teil 2: Push-Service (neues Ivy/Ant-Projekt)

### 2.1 Projektstruktur

```
push-service/
├── build.xml                          # Ant Build-Datei
├── ivy.xml                            # Ivy Dependencies
├── ivysettings.xml                    # Ivy Konfiguration
├── conf/
│   └── push-service.properties        # Server-Konfiguration
├── src/
│   └── com/ttm/push/
│       ├── PushServer.java            # Main — JDK HttpServer, kein Spring Boot
│       ├── db/
│       │   └── Database.java          # JDBC direkt (H2 + MS SQL Server)
│       ├── handler/
│       │   ├── RegistrationHandler.java  # POST /api/push/register
│       │   ├── SendHandler.java          # POST /api/push/send
│       │   └── StaticFileHandler.java    # GET /push/*
│       └── push/
│           ├── WebPushCrypto.java      # VAPID + Encryption (nur JDK Crypto!)
│           └── WebPushService.java     # Sendet Push via Web Push Protocol
├── web/
│   └── push/
│       ├── index.html                 # PWA Seite
│       ├── app.js                     # Native PushManager API (KEIN Firebase SDK!)
│       ├── sw.js                      # Service Worker (KEIN Firebase!)
│       ├── style.css                  # Mobile-optimiertes CSS
│       └── manifest.json              # PWA Manifest
└── README.md
```

### 2.2 Dependencies (ivy.xml)

**Nur diese Dependencies — KEINE weiteren:**

| Library | Version | Zweck |
|---------|---------|-------|
| com.google.code.gson:gson | 2.11.0 | JSON |
| org.slf4j:slf4j-api | 2.0.13 | Logging API |
| org.slf4j:slf4j-simple | 2.0.13 | Logging Implementierung (runtime) |
| com.h2database:h2 | 2.2.224 | Datenbank Entwicklung |
| com.microsoft.sqlserver:mssql-jdbc | 12.2.0.jre11 | Datenbank Produktion |

**KEINE Abhängigkeit auf:**
- firebase-admin (oder irgendetwas von Google Firebase)
- Spring Boot (oder irgendetwas von Spring)
- Bouncy Castle (JDK-eigene Crypto reicht für ECDH/AES-GCM/HKDF)
- web-push-java (wir implementieren Web Push selbst mit JDK-Bordmitteln)
- Jetty, Tomcat, Undertow (JDK HttpServer `com.sun.net.httpserver` reicht)

### 2.3 PushServer.java (Main-Klasse)

- Verwendet `com.sun.net.httpserver.HttpServer` (JDK built-in, seit Java 9 stabil)
- Liest Konfiguration aus `conf/push-service.properties`
- Registriert drei Contexts: `/api/push/register`, `/api/push/send`, `/push`
- ThreadPool mit `Executors.newFixedThreadPool(10)`
- Shutdown Hook für sauberes Beenden

### 2.4 Database.java (JDBC)

- Direkte JDBC-Verbindung, kein JPA/Hibernate
- Tabelle `device_registrations`:
  - `id` (BIGINT, auto-increment)
  - `player_id` (VARCHAR 50) — die Spielernummer aus der PWA
  - `endpoint` (VARCHAR 2000) — Browser Push-Endpoint URL
  - `p256dh` (VARCHAR 500) — Browser Crypto Key
  - `auth_key` (VARCHAR 200) — Browser Auth Secret
  - `created_at` (TIMESTAMP/DATETIME2)
- CREATE TABLE muss sowohl für H2 als auch für MS SQL Server funktionieren
  (IF NOT EXISTS für MSSQL, CREATE TABLE IF NOT EXISTS für H2)
- Methoden: `registerDevice()`, `getDevicesForPlayer()`, `deleteDevice()`

### 2.5 WebPushCrypto.java (VAPID + Verschlüsselung)

**WICHTIG: Komplett mit JDK-Bordmitteln, keine externe Crypto-Library!**

Verwendet:
- `java.security.KeyPairGenerator` mit `ECGenParameterSpec("secp256r1")` für ECDH (P-256)
- `javax.crypto.KeyAgreement` ("ECDH") für Shared Secret
- `javax.crypto.Mac` ("HmacSHA256") für HKDF (RFC 5869)
- `javax.crypto.Cipher` ("AES/GCM/NoPadding") für Content Encryption
- `java.security.Signature` ("SHA256withECDSA") für VAPID JWT Signatur
- `java.util.Base64` (URL-safe Encoder/Decoder)

Implementiert:
- RFC 8291 (Message Encryption for Web Push): ECDH + HKDF + AES-128-GCM
- RFC 8292 (VAPID): JWT mit ES256-Signatur
- RFC 8188 (aes128gcm Content-Coding): Header-Format für verschlüsselte Payloads
- VAPID Key-Generierung als CLI-Tool (`main()` Methode mit "generate-keys" Argument)

Die DER→Raw-RS-Konvertierung ist nötig weil JDK Signatures DER-Format liefern,
JWT aber Raw R||S (64 Bytes) erwartet.

### 2.6 WebPushService.java

- Verwendet `java.net.http.HttpClient` (JDK 11+)
- Verschlüsselt Payload mit WebPushCrypto.encrypt()
- Erstellt VAPID Authorization Header mit WebPushCrypto.createVapidAuthHeader()
- Sendet HTTP POST an den Browser-Push-Endpoint
- Return-Werte: 201/202 = OK, 410/404 = Subscription abgelaufen (löschen)

### 2.7 Handler (HTTP-Endpoints)

**RegistrationHandler (POST /api/push/register):**
- Kein Auth nötig (wird von der PWA aufgerufen)
- Request Body: `{ "playerId": "2001", "subscription": { "endpoint": "...", "keys": { "p256dh": "...", "auth": "..." } } }`
- Das `subscription`-Objekt ist exakt das, was `pushManager.subscribe().toJSON()` liefert
- Upsert: Wenn Kombination playerId+endpoint existiert, Keys aktualisieren
- CORS Headers setzen (Access-Control-Allow-Origin: *)

**SendHandler (POST /api/push/send):**
- Auth: Bearer Token im Authorization Header (muss mit push.api.key übereinstimmen)
- Request Body: `{ "playerId": "2001", "message": "Ergebnis: Mueller 3:1 Schmidt" }`
- Holt alle Geräte für den Spieler aus der DB
- Sendet an jedes Gerät per WebPushService
- Bei 410/404: Subscription aus DB löschen (Token abgelaufen)
- Response: `{ "success": true, "sent": 1, "timestamp": "Mon 14:30 GMT" }`

**StaticFileHandler (GET /push/*):**
- Liefert statische Dateien aus dem `web/push/` Verzeichnis
- MIME-Types für .html, .css, .js, .json, .png, .svg
- `/push` und `/push/` → `index.html`
- Directory Traversal Protection
- Service-Worker-Allowed Header für sw.js

### 2.8 PWA (web/push/)

**index.html:**
- Einfache mobile-optimierte Seite (max-width 480px)
- Button "Push-Benachrichtigungen aktivieren"
- Nachrichten-Liste (localStorage)
- Link auf manifest.json für PWA-Installation

**app.js — KEIN Firebase SDK, nur native Browser-APIs:**
- `VAPID_PUBLIC_KEY` Variable oben (muss nach `ant generate-vapid-keys` eingetragen werden)
- `navigator.serviceWorker.register('/push/sw.js')`
- `registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: ... })`
- `subscription.toJSON()` direkt an `/api/push/register` senden
- i18n: DE, EN, ES, FR, JA (wie im Original)
- Spieler-ID aus URL-Parameter `?player=2001`
- Nachrichten-Anzeige aus localStorage
- Empfang von Foreground-Messages via `navigator.serviceWorker.addEventListener('message', ...)`

**sw.js — Service Worker ohne Firebase:**
- `self.addEventListener('push', ...)` — empfängt Push-Events direkt vom Browser
- `event.data.json()` zum Parsen der Nachricht
- `self.registration.showNotification(...)` zum Anzeigen
- `self.clients.matchAll(...)` + `client.postMessage(...)` für Foreground-Weiterleitung
- `self.addEventListener('notificationclick', ...)` für Klick-Handling

**style.css:**
- Clean, mobile-first Design
- Blau-Ton (#1a5276) als Hauptfarbe
- Button-States: normal, hover, disabled, registered (grün)

**manifest.json:**
- PWA-Manifest mit name, short_name, start_url, icons
- display: standalone

### 2.9 Konfiguration (conf/push-service.properties)

```properties
server.port=8080
push.api.key=changeme

# VAPID Keys (einmalig generieren mit: ant generate-vapid-keys)
vapid.public.key=HIER_EINTRAGEN
vapid.private.key=HIER_EINTRAGEN
vapid.subject=mailto:admin@ttm.co.at

# H2 (Entwicklung):
db.url=jdbc:h2:file:./data/pushdb
db.username=sa
db.password=

# MS SQL Server (Produktion) — diese Zeilen aktivieren, H2 auskommentieren:
# db.url=jdbc:sqlserver://SERVER:1433;databaseName=DBNAME;encrypt=true;trustServerCertificate=true
# db.username=BENUTZER
# db.password=PASSWORT

web.directory=web
```

### 2.10 Build (build.xml)

- `ant resolve` — Ivy Dependencies laden
- `ant compile` — Kompilieren
- `ant jar` — JAR erstellen
- `ant dist` — Verteilbare Distribution erstellen (JAR + lib/ + web/ + conf/ + Start-Scripts)
- `ant generate-vapid-keys` — VAPID Keys generieren (ruft WebPushCrypto.main() auf)
- `ant clean` / `ant clean-all`

Die Distribution (`dist/`) enthält:
- `push-service.jar`
- `lib/` (alle JARs)
- `web/push/` (PWA Dateien)
- `conf/push-service.properties`
- `start.sh` / `start.bat`

### 2.11 README.md

Soll enthalten:
- Schnellstart-Anleitung (3 Schritte: Keys generieren, eintragen, starten)
- Lokaler Test mit curl-Beispiel
- Deployment-Anleitung für ttm.co.at (Reverse Proxy Konfiguration)
- QR-Code URL Format: `https://www.ttm.co.at/push/?player={spielernummer}`
- API-Dokumentation (Register + Send Endpoints)
- Datenbank-Konfiguration (H2 ↔ MS SQL Server)
- Dependency-Liste (nur 4-5 JARs, keine Firebase, kein Spring)

---

## Teil 3: Reihenfolge der Umsetzung

### Schritt 1: Altes push-service/ Verzeichnis löschen
Das gesamte `push-service/` Verzeichnis aus Commit 4a1c143 entfernen
(Maven/Spring Boot/Firebase Version).

### Schritt 2: Neues push-service/ Projekt anlegen
Alle Dateien aus Teil 2 erstellen.

### Schritt 3: Bug-Fix in PushHTTPGateway.java
`super(id, "Push Notification Gateway")` → `super(id)`

### Schritt 4: Recipient-Mapping in PushHTTPGateway.java
Rückwärts-Suche implementieren (Telefonnummer → plNr) wie in 1.2 beschrieben.
Dazu muss PushHTTPGateway oder PushGateway.java Zugriff auf die Turnier-DB bekommen.

### Schritt 5: Kompilieren und testen
- `cd push-service && ant dist`
- Push-Service starten
- Browser auf `http://localhost:8080/push/?player=TEST01`
- Registrieren, dann per curl eine Nachricht senden

---

## Zusammenfassung: Was NICHT verwendet werden darf

- ❌ Firebase (kein firebase-admin, kein Firebase JS SDK, kein Firebase Console)
- ❌ Spring Boot (kein Spring Framework, kein Tomcat embedded)
- ❌ Maven (kein pom.xml — nur Ivy + Ant)
- ❌ Bouncy Castle oder andere externe Crypto-Libraries
- ❌ web-push-java Library (wir implementieren Web Push selbst)
- ❌ Jetty, Undertow, Netty oder andere HTTP-Server

## Was VERWENDET werden soll

- ✅ JDK HttpServer (`com.sun.net.httpserver`)
- ✅ JDK Crypto (`java.security`, `javax.crypto`)
- ✅ JDK HttpClient (`java.net.http`)
- ✅ Gson für JSON
- ✅ SLF4J für Logging
- ✅ H2 / MS SQL Server via JDBC
- ✅ Apache Ivy + Apache Ant
- ✅ Native Browser Web Push API (PushManager, Service Worker)
