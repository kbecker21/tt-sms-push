# TTM Push-Benachrichtigungen — Komplettdokumentation

Dieses Dokument beschreibt Einrichtung, Konfiguration, Test und Betrieb der beiden
Projekte **push-service** (Web-Push-Server) und **SMSCenterExt** (SMS-Gateway mit Push-Erweiterung).

Stand: 2026-03-21

---

## Inhaltsverzeichnis

1. [Überblick und Architektur](#1-überblick-und-architektur)
2. [Voraussetzungen](#2-voraussetzungen)
3. [push-service — Einrichtung und Betrieb](#3-push-service--einrichtung-und-betrieb)
   - 3.1 [Projektstruktur](#31-projektstruktur)
   - 3.2 [Dependencies (Abhängigkeiten)](#32-dependencies-abhängigkeiten)
   - 3.3 [Bauen (Build)](#33-bauen-build)
   - 3.4 [VAPID-Keys generieren](#34-vapid-keys-generieren)
   - 3.5 [Konfiguration (push-service.properties)](#35-konfiguration-push-serviceproperties)
   - 3.6 [Logging-Konfiguration (log4j.properties)](#36-logging-konfiguration-log4jproperties)
   - 3.7 [Datenbank einrichten](#37-datenbank-einrichten)
   - 3.8 [Server starten](#38-server-starten)
   - 3.9 [Firewall-Konfiguration](#39-firewall-konfiguration)
4. [push-service — API-Referenz](#4-push-service--api-referenz)
   - 4.1 [POST /api/push/register](#41-post-apipushregister)
   - 4.2 [POST /api/push/send](#42-post-apipushsend)
   - 4.3 [GET /api/push/status](#43-get-apipushstatus)
   - 4.4 [POST /api/push/unregister](#44-post-apipushunregister)
   - 4.5 [GET /push/* (Statische Dateien)](#45-get-push-statische-dateien)
5. [push-service — PWA (Webseite im Browser)](#5-push-service--pwa-webseite-im-browser)
   - 5.1 [Registrierungs-Ablauf](#51-registrierungs-ablauf)
   - 5.2 [Service Worker](#52-service-worker)
   - 5.3 [Manifest und Icons](#53-manifest-und-icons)
   - 5.4 [Mehrsprachigkeit](#54-mehrsprachigkeit)
6. [push-service — Testen](#6-push-service--testen)
   - 6.1 [Schnelltest (Einzelspieler)](#61-schnelltest-einzelspieler)
   - 6.2 [Erweiterter Test (Mehrere Spieler)](#62-erweiterter-test-mehrere-spieler)
   - 6.3 [Abmeldefunktion testen](#63-abmeldefunktion-testen)
   - 6.4 [Browser schliessen und wieder öffnen](#64-browser-schliessen-und-wieder-öffnen)
   - 6.5 [Datenbank prüfen](#65-datenbank-prüfen)
   - 6.6 [Fehlersuche (Troubleshooting)](#66-fehlersuche-troubleshooting)
7. [SMSCenterExt — Einrichtung und Betrieb](#7-smscenterext--einrichtung-und-betrieb)
   - 7.1 [Projektstruktur](#71-projektstruktur)
   - 7.2 [Dependencies](#72-dependencies)
   - 7.3 [Bauen](#73-bauen)
   - 7.4 [Push-Gateway konfigurieren](#74-push-gateway-konfigurieren)
   - 7.5 [Empfänger-Zuordnung (Telefonnummer → Spielernummer)](#75-empfänger-zuordnung-telefonnummer--spielernummer)
8. [SMSCenterExt — Testen](#8-smscenterext--testen)
9. [Zusammenspiel beider Projekte](#9-zusammenspiel-beider-projekte)
   - 9.1 [Gesamtablauf einer Push-Nachricht](#91-gesamtablauf-einer-push-nachricht)
   - 9.2 [Integrations-Checkliste](#92-integrations-checkliste)
10. [Deployment auf dem Server (ttm.co.at)](#10-deployment-auf-dem-server-ttmcoat)
11. [Bekannte Einschränkungen und Hinweise](#11-bekannte-einschränkungen-und-hinweise)
12. [Technische Details (Kryptografie)](#12-technische-details-kryptografie)

---

## 1. Überblick und Architektur

Das System besteht aus drei Komponenten:

```
SMSCenterExt (Desktop-App)              push-service (eigener Prozess)        Browser (Spieler)
┌──────────────────────────┐  HTTP POST  ┌───────────────────────────┐  Web Push  ┌───────────────┐
│ PushHTTPGateway.java     │────────────>│ POST /api/push/send       │──────────>│ Service Worker │
│ (OkHttp + Gson)          │             │   (Bearer-Token Auth)     │           │ (sw.js)        │
│                          │             │                           │           │                │
│ Telefonnr → Spielernr    │             │ POST /api/push/register   │<──────────│ PWA (app.js)   │
│ (DB-Lookup)              │             │ GET  /api/push/status     │<──────────│                │
└──────────────────────────┘             │ POST /api/push/unregister │<──────────│                │
                                         │                           │           └────────────────┘
                                         │ GET  /push/* (PWA-Dateien)│
                                         │                           │
                                         │ H2 / MS SQL Server DB     │
                                         └───────────────────────────┘
```

**push-service**: Eigenständiger Java-Webserver, der:
- die PWA-Webseite für Spieler ausliefert
- Browser-Registrierungen entgegennimmt und in der DB speichert
- Push-Nachrichten verschlüsselt und an die Browser-Endpoints sendet

**SMSCenterExt**: Desktop-Anwendung für Turnierleiter, die:
- Spielergebnisse per SMS und/oder Push verschickt
- über das PushHTTPGateway den push-service per HTTP ansteuert
- automatisch Telefonnummern in Spielernummern auflöst

**Browser (PWA)**: Webseite, die:
- einen Service Worker registriert, der Push-Nachrichten empfängt
- Benachrichtigungen als System-Notifications anzeigt
- auch bei geschlossenem Tab funktioniert (solange der Browser läuft)

---

## 2. Voraussetzungen

| Komponente | Minimum | Empfohlen |
|------------|---------|-----------|
| Java (JDK) | 17 | 21 |
| Apache Ant | 1.10+ | 1.10.14 |
| Apache Ivy | wird automatisch heruntergeladen | — |
| Datenbank | H2 (eingebaut) ODER MS SQL Server 2019+ | MS SQL Server für Produktion |
| Browser | Chrome 80+, Firefox 78+, Edge 80+ | Chrome (aktuellste Version) |
| Betriebssystem | Windows 10+, Linux, macOS | — |
| NetBeans (optional) | 12+ | für Debugging in der IDE |
| Git Bash (optional) | — | für curl-Tests unter Windows |

**Wichtig**: Safari unterstützt Web Push erst ab Version 16.4 (macOS Ventura / iOS 16.4).

---

## 3. push-service — Einrichtung und Betrieb

### 3.1 Projektstruktur

```
push-service/
├── build.xml                       # Ant-Build-Datei
├── ivy.xml                         # Ivy-Dependencies
├── conf/
│   ├── push-service.properties     # Server-Konfiguration
│   └── log4j.properties            # Logging-Konfiguration
├── src/
│   └── com/ttm/push/
│       ├── PushServer.java         # Main-Klasse (HTTP-Server)
│       ├── db/
│       │   └── Database.java       # Datenbankzugriff (H2 + SQL Server)
│       ├── handler/
│       │   ├── RegistrationHandler.java    # POST /api/push/register
│       │   ├── SendHandler.java            # POST /api/push/send
│       │   ├── StatusHandler.java          # GET  /api/push/status
│       │   ├── UnregisterHandler.java      # POST /api/push/unregister
│       │   └── StaticFileHandler.java      # GET  /push/*
│       └── push/
│           ├── WebPushService.java         # Push-Versand (RFC 8030)
│           └── WebPushCrypto.java          # Verschlüsselung + VAPID
├── web/
│   └── push/
│       ├── index.html              # Registrierungs-Seite
│       ├── app.js                  # Browser-Logik
│       ├── sw.js                   # Service Worker
│       ├── style.css               # Styling
│       ├── manifest.json           # PWA-Manifest
│       ├── icon-192.png            # App-Icon 192x192
│       └── icon-512.png            # App-Icon 512x512
├── build/                          # Kompilierte Klassen (generiert)
├── dist/                           # Distribution (generiert)
├── lib/                            # JAR-Dependencies (generiert)
└── data/                           # H2-Datenbankdatei (generiert, nur bei H2)
```

### 3.2 Dependencies (Abhängigkeiten)

Alle Dependencies werden automatisch von Ivy heruntergeladen:

| Library | Version | Zweck |
|---------|---------|-------|
| com.google.code.gson:gson | 2.11.0 | JSON-Verarbeitung |
| org.slf4j:slf4j-log4j12 | 1.7.32 | Logging (SLF4J + Log4J) |
| com.h2database:h2 | 2.2.224 | Datenbank für Entwicklung |
| com.microsoft.sqlserver:mssql-jdbc | 12.2.0.jre11 | Datenbank für Produktion |

**Bewusst NICHT verwendet**: Firebase, Spring Boot, Bouncy Castle, Maven.
Alle Kryptografie-Operationen nutzen JDK-Bordmittel (`java.security`, `javax.crypto`).

### 3.3 Bauen (Build)

```bash
cd push-service

# 1. Dependencies herunterladen (nur beim ersten Mal nötig)
ant resolve

# 2. Kompilieren
ant compile

# 3. JAR erstellen
ant jar

# 4. Vollständige Distribution erstellen (JAR + lib + web + conf + Startscripts)
ant dist
```

**Alle Ant-Targets im Überblick:**

| Target | Beschreibung |
|--------|-------------|
| `ant resolve` | Ivy-Dependencies herunterladen nach `lib/` |
| `ant compile` | Java-Quellcode kompilieren |
| `ant jar` | `push-service.jar` erstellen (mit Main-Class im Manifest) |
| `ant dist` | Distribution zusammenstellen in `dist/` |
| `ant generate-vapid-keys` | VAPID-Schlüsselpaar generieren (siehe 3.4) |
| `ant clean` | `build/` und `dist/` löschen |
| `ant clean-all` | `build/`, `dist/` und `lib/` löschen |

### 3.4 VAPID-Keys generieren

VAPID-Keys (Voluntary Application Server Identification) werden für die Verschlüsselung
und Authentifizierung der Push-Nachrichten benötigt. Sie müssen **einmalig** generiert werden.

```bash
ant generate-vapid-keys
```

Ausgabe (Beispiel):
```
=== VAPID Keys Generated ===
Public Key:  BM_6rWcY-Ol77wUT9nvz1YQKL18v_rfTIaKzFLOrdYGyqrfVKtOK_5GH-j4F6SBs7sXz35NYwHgNTFQBrcaaDVw
Private Key: zjXUUK3_OoJeobk5gEplbnj8Fo3XHtr_LZ9IbIfRKas
```

**Beide Keys müssen an zwei Stellen eingetragen werden:**

1. **Server-Konfiguration** `conf/push-service.properties`:
   ```properties
   vapid.public.key=BM_6rWcY-Ol77wUT...
   vapid.private.key=zjXUUK3_OoJeobk...
   ```

2. **Browser-JavaScript** `web/push/app.js` (Zeile 2):
   ```javascript
   const VAPID_PUBLIC_KEY = 'BM_6rWcY-Ol77wUT...';
   ```

   **Achtung**: In `app.js` wird nur der **Public Key** eingetragen! Der Private Key
   bleibt ausschließlich auf dem Server.

**Gültigkeit**: VAPID-Keys laufen nicht ab. Sie können unbegrenzt verwendet werden.
Bei einem Wechsel der Keys müssen sich alle Spieler neu registrieren.

### 3.5 Konfiguration (push-service.properties)

Datei: `conf/push-service.properties`

```properties
# ── Server ──────────────────────────────────────────────
server.port=8080
# Port, auf dem der HTTP-Server lauscht.
# Für lokale Tests: 8080
# Für Produktion hinter Reverse Proxy: beliebig (z.B. 8080)

# ── API-Sicherheit ─────────────────────────────────────
push.api.key=changeme
# Bearer-Token für den /api/push/send Endpoint.
# UNBEDINGT in Produktion ändern!
# Dieses Token muss auch im SMSCenterExt als apiKey eingetragen werden.

# ── VAPID-Schlüssel ────────────────────────────────────
vapid.public.key=BM_6rWcY-Ol77wUT9nvz1YQKL18v_rfTIaKzFLOrdYGyqrfVKtOK_5GH-j4F6SBs7sXz35NYwHgNTFQBrcaaDVw
vapid.private.key=zjXUUK3_OoJeobk5gEplbnj8Fo3XHtr_LZ9IbIfRKas
vapid.subject=mailto:admin@ttm.co.at
# vapid.subject: Kontakt-E-Mail gemäß RFC 8292.
# Wird nie an Spieler angezeigt, dient zur Identifikation beim Push-Dienst.

# ── Datenbank ──────────────────────────────────────────
# Option A: H2 (Entwicklung, kein extra Server nötig)
# db.url=jdbc:h2:file:./data/pushdb
# db.username=sa
# db.password=

# Option B: MS SQL Server (Produktion)
db.url=jdbc:sqlserver://localhost:1433;databaseName=TESTTURNIER;encrypt=true;trustServerCertificate=true
db.username=sa
db.password=TTM

# ── Web-Verzeichnis ────────────────────────────────────
web.directory=web
# Pfad zum Verzeichnis, das die PWA-Dateien enthält (relativ zum Arbeitsverzeichnis).
```

**Zwischen H2 und SQL Server wechseln**: Einfach die gewünschten `db.*`-Zeilen
ein-/auskommentieren. Die Tabelle `device_registrations` wird beim Start automatisch
angelegt (falls nicht vorhanden).

### 3.6 Logging-Konfiguration (log4j.properties)

Datei: `conf/log4j.properties`

```properties
# Root logger option
log4j.rootLogger=INFO, stdout

# Redirect log messages to console
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n
```

**Log-Level anpassen**: `INFO` zeigt normale Betriebsmeldungen. Für Fehlersuche
auf `DEBUG` setzen (erzeugt deutlich mehr Ausgabe).

### 3.7 Datenbank einrichten

#### Option A: H2 (Entwicklung)

Nichts zu tun. H2 legt die Datenbankdatei automatisch unter `data/pushdb.mv.db` an.

**Nachteil**: Kein externer DB-Client verfügbar (Squirrel o.ä. schwer zum Laufen zu bringen).

#### Option B: MS SQL Server (Empfohlen für Tests und Produktion)

1. Sicherstellen, dass SQL Server läuft und erreichbar ist
2. Eine Datenbank anlegen (z.B. `TESTTURNIER`) oder eine vorhandene verwenden
3. Die Tabelle `device_registrations` wird beim ersten Start automatisch angelegt:

   ```sql
   -- Wird automatisch erzeugt, hier nur zur Referenz:
   IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'device_registrations')
   CREATE TABLE device_registrations (
       id BIGINT IDENTITY(1,1) PRIMARY KEY,
       player_id VARCHAR(50) NOT NULL,
       endpoint VARCHAR(2000) NOT NULL,
       p256dh VARCHAR(500) NOT NULL,
       auth_key VARCHAR(200) NOT NULL,
       created_at DATETIME2 DEFAULT GETDATE()
   )
   ```

4. Zugangsdaten in `push-service.properties` eintragen (siehe 3.5)

**Daten prüfen mit SSMS**:
```sql
SELECT * FROM device_registrations ORDER BY created_at DESC;
```

### 3.8 Server starten

#### Aus NetBeans

Projekt öffnen → Rechtsklick → "Run" (oder F6). Der PushServer startet und gibt
in der Konsole aus:

```
2026-03-21 10:00:00 INFO  PushServer:56 - Push server started on port 8080
2026-03-21 10:00:00 INFO  PushServer:57 - PWA available at http://localhost:8080/push/
```

#### Aus der Kommandozeile

```bash
cd push-service

# Variante 1: Direkt aus dem Build
java -cp "build;lib/*" com.ttm.push.PushServer

# Variante 2: Aus der Distribution
cd dist
start.bat          # Windows
./start.sh         # Linux/macOS
```

#### Aus der Distribution (dist/)

Nach `ant dist` enthält `dist/`:
```
dist/
├── push-service.jar
├── lib/            (alle JARs)
├── web/push/       (PWA-Dateien)
├── conf/push-service.properties
├── start.bat       (Windows)
└── start.sh        (Linux)
```

Das gesamte `dist/`-Verzeichnis kann auf einen anderen Rechner kopiert werden.

### 3.9 Firewall-Konfiguration

Der Server-Port (Standard: 8080) muss in der Windows-Firewall freigegeben sein,
damit andere Geräte im Netzwerk darauf zugreifen können.

**Prüfen, ob ein Port frei ist** (PowerShell):
```powershell
Test-NetConnection -ComputerName localhost -Port 8080
```

**Port freigeben** (PowerShell als Administrator):
```powershell
New-NetFirewallRule -DisplayName "Push-Service Port 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
```

**Port 80 statt 8080**: Möglich, wenn nicht bereits belegt (z.B. durch IIS). In
`push-service.properties` dann `server.port=80` setzen. Unter Linux benötigt Port 80
Root-Rechte oder `setcap`.

---

## 4. push-service — API-Referenz

### 4.1 POST /api/push/register

Registriert ein Gerät (Browser) für einen Spieler. Wird von der PWA aufgerufen.

**Authentifizierung**: Keine (öffentlicher Endpoint)

**Request**:
```
POST /api/push/register HTTP/1.1
Content-Type: application/json

{
    "playerId": "2001",
    "subscription": {
        "endpoint": "https://fcm.googleapis.com/fcm/send/...",
        "keys": {
            "p256dh": "BNcRdreALRFXTkOOUHK1...",
            "auth": "tBHItJI5svbpC7..."
        }
    }
}
```

- `playerId`: Spielernummer (String, wie im URL-Parameter `?player=2001`)
- `subscription`: Exakt das Objekt, das `pushManager.subscribe().toJSON()` liefert

**Response (200 OK)**:
```json
{
    "success": true,
    "message": "Device registered for player 2001"
}
```

**Verhalten**: Upsert — bei bestehender Kombination `playerId + endpoint` werden
die Schlüssel aktualisiert; bei neuer Kombination wird eingefügt.

---

### 4.2 POST /api/push/send

Sendet eine Push-Nachricht an alle registrierten Geräte eines Spielers.

**Authentifizierung**: Bearer Token (muss mit `push.api.key` übereinstimmen)

**Request**:
```
POST /api/push/send HTTP/1.1
Authorization: Bearer changeme
Content-Type: application/json

{
    "playerId": "2001",
    "message": "Ergebnis: Mueller 3:1 Schmidt"
}
```

**Response (200 OK)**:
```json
{
    "success": true,
    "sent": 1,
    "timestamp": "Fri 14:30 CET"
}
```

- `sent`: Anzahl der Geräte, an die erfolgreich gesendet wurde
- Wenn `sent: 0` und Spieler nicht registriert: zusätzlich `"message": "No devices registered for player 2001"`

**Verhalten bei abgelaufenen Subscriptions**: Wenn der Browser-Push-Endpoint mit
HTTP 410 (Gone) oder 404 antwortet, wird die Registrierung automatisch aus der DB gelöscht.

**curl-Beispiel** (Git Bash):
```bash
curl -X POST http://localhost:8080/api/push/send \
  -H "Authorization: Bearer changeme" \
  -H "Content-Type: application/json" \
  -d '{"playerId": "TEST01", "message": "Ergebnis: Mueller 3:1 Schmidt"}'
```

**curl-Beispiel** (Windows CMD — Anführungszeichen beachten):
```cmd
curl -X POST http://localhost:8080/api/push/send -H "Authorization: Bearer changeme" -H "Content-Type: application/json" -d "{\"playerId\": \"TEST01\", \"message\": \"Ergebnis: Mueller 3:1 Schmidt\"}"
```

---

### 4.3 GET /api/push/status

Prüft, ob ein Spieler mit einem bestimmten Endpoint auf dem Server registriert ist.
Wird von der PWA bei jedem Seitenaufruf verwendet, um den Browser-Cache mit dem
Server-Zustand abzugleichen.

**Authentifizierung**: Keine

**Request**:
```
GET /api/push/status?playerId=2001&endpoint=https%3A%2F%2Ffcm.googleapis.com%2F... HTTP/1.1
```

- `endpoint` muss URL-encoded sein (wird von `app.js` automatisch gemacht)

**Response (200 OK)**:
```json
{
    "registered": true
}
```

---

### 4.4 POST /api/push/unregister

Meldet einen Spieler von Push-Nachrichten ab. Löscht die Registrierung aus der Datenbank.

**Authentifizierung**: Keine (benutzerinitiiert)

**Request**:
```
POST /api/push/unregister HTTP/1.1
Content-Type: application/json

{
    "playerId": "2001",
    "subscription": {
        "endpoint": "https://fcm.googleapis.com/fcm/send/..."
    }
}
```

**Response (200 OK)**:
```json
{
    "success": true,
    "message": "Player 2001 unregistered"
}
```

**Hinweis**: Die Browser-Subscription wird bewusst NICHT gelöscht, da andere Spieler
auf demselben Gerät sie noch verwenden könnten.

---

### 4.5 GET /push/* (Statische Dateien)

Liefert die PWA-Dateien (HTML, JS, CSS, Icons, Manifest) aus.

- `/push/` oder `/push` → `index.html`
- `/push/app.js` → JavaScript
- `/push/sw.js` → Service Worker (mit Header `Service-Worker-Allowed: /`)
- `/push/manifest.json` → PWA-Manifest
- usw.

**Sicherheit**: Directory-Traversal-Schutz ist implementiert (Pfade werden normalisiert).

---

## 5. push-service — PWA (Webseite im Browser)

### 5.1 Registrierungs-Ablauf

Wenn ein Spieler die URL aufruft (z.B. `http://localhost:8080/push/?player=2001`):

```
1. Seite wird geladen
   ↓
2. Service Worker (sw.js) wird registriert
   ↓
3. Prüfung: Hat der Browser bereits eine Push-Subscription?
   │
   ├─ JA → Server fragen: "Ist Spieler 2001 mit diesem Endpoint registriert?"
   │        (GET /api/push/status)
   │        │
   │        ├─ Server sagt JA → "Registriert ✓" anzeigen + Abmelde-Button
   │        │
   │        └─ Server sagt NEIN → Automatisch re-registrieren
   │                              (POST /api/push/register)
   │                              → "Registriert ✓" anzeigen + Abmelde-Button
   │
   └─ NEIN → "Aktivieren"-Button anzeigen
              │
              └─ Spieler klickt Button
                 ↓
                 Browser fragt: "Benachrichtigungen erlauben?"
                 │
                 ├─ Erlaubt → Subscription erstellen → An Server senden
                 │             → "Registriert ✓" anzeigen
                 │
                 └─ Verweigert → Fehlermeldung anzeigen
```

**Kernverbesserung gegenüber der alten Version**: Die Seite prüft jetzt IMMER beim
Server, ob die Registrierung tatsächlich existiert. Dadurch werden falsche
"Registriert"-Anzeigen vermieden.

### 5.2 Service Worker

Der Service Worker (`sw.js`) läuft als Hintergrundprozess im Browser:

- **Push-Empfang**: Empfängt Push-Nachrichten auch wenn der Tab geschlossen ist
- **Benachrichtigungen**: Zeigt Windows-/System-Notifications an (mit Icon, Vibration, Ton)
- **Weiterleitung**: Leitet empfangene Nachrichten an offene Tabs weiter (dort werden
  sie in der Nachrichtenliste angezeigt)
- **Klick-Handling**: Beim Klick auf eine Notification wird der `/push/`-Tab fokussiert
  oder ein neuer geöffnet

**Lebenszyklus**: Browser stoppen und starten Service Worker automatisch. Ein
"gestoppter" SW ist normal und kein Fehler — Push-Events wecken ihn sofort auf.

**chrome://serviceworker-internals/** zeigt alle registrierten SWs. Status "stopped"
ist der Normalzustand wenn gerade keine Push-Nachricht verarbeitet wird.

### 5.3 Manifest und Icons

Die Datei `manifest.json` macht die Webseite zu einer Progressive Web App (PWA):

```json
{
    "name": "TTM Push-Benachrichtigungen",
    "short_name": "TTM Push",
    "start_url": "/push/",
    "display": "standalone",
    "theme_color": "#1a5276",
    "icons": [
        { "src": "icon-192.png", "sizes": "192x192" },
        { "src": "icon-512.png", "sizes": "512x512" }
    ]
}
```

**Wofür die Icons verwendet werden**:
- Als App-Icon wenn die PWA zum Startbildschirm hinzugefügt wird ("Add to Home Screen")
- Als Icon in Push-Benachrichtigungen (das kleine Bild neben dem Text)
- In der Browser-Tab-Leiste (als Favicon)

**Hinweis**: Die Icon-Dateien `icon-192.png` und `icon-512.png` müssen im Verzeichnis
`web/push/` liegen. Wenn sie fehlen, funktioniert alles trotzdem, aber ohne Icon.

### 5.4 Mehrsprachigkeit

Die PWA erkennt die Browser-Sprache automatisch und unterstützt:

| Sprache | Code | Beispiel "Aktivieren"-Button |
|---------|------|------------------------------|
| Deutsch | de | Push-Benachrichtigungen aktivieren |
| Englisch | en | Enable Push Notifications |
| Spanisch | es | Activar Notificaciones Push |
| Französisch | fr | Activer les Notifications Push |
| Japanisch | ja | プッシュ通知を有効にする |

Fallback bei unbekannter Sprache: Englisch.

---

## 6. push-service — Testen

### 6.1 Schnelltest (Einzelspieler)

**Schritt 1**: push-service starten (siehe 3.8)

Erwartete Konsolenausgabe:
```
2026-03-21 10:00:00 INFO  Database:31 - Database initialized: jdbc:sqlserver://...
2026-03-21 10:00:00 INFO  PushServer:56 - Push server started on port 8080
2026-03-21 10:00:00 INFO  PushServer:57 - PWA available at http://localhost:8080/push/
```

**Schritt 2**: Im Browser öffnen:
```
http://localhost:8080/push/?player=TEST01
```

- Die Seite zeigt "Spieler: TEST01" und einen blauen "Aktivieren"-Button

**Schritt 3**: Auf "Push-Benachrichtigungen aktivieren" klicken

- Browser fragt nach Berechtigung → "Erlauben" wählen
- Button wird grün und zeigt "Registriert ✓"
- Darunter erscheint der "Abmelden"-Button
- In der Server-Konsole: `INFO RegistrationHandler:64 - Device registered for player TEST01`

**Schritt 4**: Push-Nachricht senden (Git Bash):

```bash
curl -X POST http://localhost:8080/api/push/send \
  -H "Authorization: Bearer changeme" \
  -H "Content-Type: application/json" \
  -d '{"playerId": "TEST01", "message": "Ergebnis: Mueller 3:1 Schmidt"}'
```

Erwartete Antwort:
```json
{"success":true,"sent":1,"timestamp":"Fri 10:05 CET"}
```

**Schritt 5**: Prüfen:
- Im Browserfenster erscheint die Nachricht in der Nachrichtenliste
- Windows zeigt rechts unten eine Notification-Popup

### 6.2 Erweiterter Test (Mehrere Spieler)

Mehrere Spieler im **selben Browser** (Chrome) testen:

**Tab 1**: `http://localhost:8080/push/?player=TEST01` → Registrieren

**Tab 2** (neuer Tab): `http://localhost:8080/push/?player=TEST02`
- Die Seite prüft automatisch beim Server → Server kennt TEST02 nicht
- Die bestehende Browser-Subscription wird automatisch für TEST02 registriert
- "Registriert ✓" wird angezeigt

**Prüfung** in der Datenbank:
```sql
SELECT * FROM device_registrations;
-- Es sollten 2 Einträge sein:
-- TEST01 mit endpoint https://fcm...
-- TEST02 mit demselben endpoint
```

**Push-Test**:
```bash
# An TEST01 senden
curl -X POST http://localhost:8080/api/push/send \
  -H "Authorization: Bearer changeme" \
  -H "Content-Type: application/json" \
  -d '{"playerId": "TEST01", "message": "Nachricht fuer TEST01"}'

# An TEST02 senden
curl -X POST http://localhost:8080/api/push/send \
  -H "Authorization: Bearer changeme" \
  -H "Content-Type: application/json" \
  -d '{"playerId": "TEST02", "message": "Nachricht fuer TEST02"}'
```

**Hinweis**: Da beide Spieler im selben Browser denselben Service Worker teilen,
erscheinen beide Nachrichten als Windows-Notification. In der Nachrichtenliste im
jeweiligen Tab erscheint die Nachricht nur bei dem Tab, der gerade offen ist.

### 6.3 Abmeldefunktion testen

1. Seite `http://localhost:8080/push/?player=TEST01` öffnen
2. Spieler ist registriert → "Registriert ✓" und "Abmelden"-Button sichtbar
3. Auf "Abmelden" klicken
4. Meldung "Abmeldung erfolgreich." erscheint
5. Der "Aktivieren"-Button ist wieder sichtbar
6. Die Nachrichtenliste ist gelöscht

**Prüfung in der DB**:
```sql
SELECT * FROM device_registrations WHERE player_id = 'TEST01';
-- Sollte leer sein
```

**Push-Test** nach Abmeldung:
```bash
curl -X POST http://localhost:8080/api/push/send \
  -H "Authorization: Bearer changeme" \
  -H "Content-Type: application/json" \
  -d '{"playerId": "TEST01", "message": "Diese kommt nicht an"}'
```
Antwort: `{"success":true,"sent":0,"message":"No devices registered for player TEST01"}`

### 6.4 Browser schliessen und wieder öffnen

1. Spieler TEST01 registrieren
2. **Browser komplett schliessen** (nicht nur den Tab!)
3. Browser neu öffnen und `http://localhost:8080/push/?player=TEST01` aufrufen
4. Die Seite prüft automatisch beim Server → "Registriert ✓"

**Während der Browser geschlossen war**:
- Push-Nachrichten, die gesendet werden während der Browser geschlossen ist,
  gehen NICHT verloren — der Push-Dienst (FCM/Mozilla) hält sie vor (TTL: 24 Stunden)
- Beim nächsten Start des Browsers werden sie als System-Notifications zugestellt

### 6.5 Datenbank prüfen

**SQL Server Management Studio (SSMS)**:

```sql
-- Alle Registrierungen anzeigen
SELECT id, player_id, LEFT(endpoint, 80) AS endpoint_kurz, created_at
FROM device_registrations
ORDER BY created_at DESC;

-- Registrierungen für einen bestimmten Spieler
SELECT * FROM device_registrations WHERE player_id = 'TEST01';

-- Anzahl registrierter Geräte pro Spieler
SELECT player_id, COUNT(*) AS geraete
FROM device_registrations
GROUP BY player_id;

-- Alle Registrierungen löschen (für Neustart)
DELETE FROM device_registrations;

-- Tabelle komplett neu anlegen lassen (Tabelle löschen, Server neu starten)
DROP TABLE device_registrations;
```

### 6.6 Fehlersuche (Troubleshooting)

| Problem | Mögliche Ursache | Lösung |
|---------|------------------|--------|
| Seite zeigt "Registriert" aber DB ist leer | Alter Zustand im Browser-Cache | Ctrl+F5 (Hard Reload) oder Service Worker in `chrome://serviceworker-internals` auf "Unregister" klicken |
| Button bleibt ausgegraut | Browser unterstützt kein Web Push | Anderen Browser verwenden (Chrome/Firefox/Edge) |
| "Berechtigung verweigert" | Notification-Permission auf "Block" | In Chrome: Schloss-Icon in URL-Leiste → Benachrichtigungen → Erlauben |
| Push kommt nicht an | Subscription abgelaufen (410) | Spieler muss sich neu registrieren; alte Einträge werden automatisch gelöscht |
| Server startet nicht | Port belegt | `netstat -aon | findstr :8080` prüfen, anderen Port konfigurieren |
| "VAPID keys not configured" | Keys nicht eingetragen | Siehe Abschnitt 3.4 |
| Mehrere Spieler funktionieren nicht | Alte app.js im Cache | Ctrl+F5 oder Browser-Cache leeren |
| Nachricht kommt doppelt | Mehrere Registrierungen in DB | `SELECT * FROM device_registrations` prüfen, Duplikate löschen |

**Browser-Entwicklertools nutzen (F12)**:
- **Console**: JavaScript-Fehler und Log-Meldungen
- **Network**: HTTP-Requests an `/api/push/*` prüfen (Status 200?)
- **Application → Service Workers**: SW-Status, Push-Subscription anzeigen
- **Application → Local Storage**: Gespeicherte Nachrichten unter `push_messages_TEST01`

---

## 7. SMSCenterExt — Einrichtung und Betrieb

### 7.1 Projektstruktur

```
SMSCenterExt/
├── build.xml                       # NetBeans/Ant Build-Datei
├── ivy.xml                         # Ivy-Dependencies
├── ivysettings.xml                 # Ivy-Konfiguration
├── manifest.mf                     # JAR-Manifest
├── SMSCenter.iss                   # Inno Setup Installer-Script
├── src/smscenter/
│   ├── SMSCenter.java              # Hauptanwendung (Swing Desktop-App)
│   ├── database/                   # Datenbankzugriff und Modelle
│   │   ├── Database.java
│   │   ├── Competition.java
│   │   ├── Group.java
│   │   ├── Match.java
│   │   ├── Player.java
│   │   └── Phone.java
│   ├── gui/                        # Swing-Oberfläche
│   │   ├── MainFrame.java
│   │   ├── BasePanel.java
│   │   └── settings/
│   │       ├── GatewaySettings.java
│   │       ├── Settings.java
│   │       ├── PushGatewaySettings.java       # Push-Gateway-Konfiguration
│   │       └── PushGatewaySettingsPanel.java   # Push-Gateway UI-Panel
│   └── smsserver/
│       ├── SMSServer.java          # SMS-Server-Kern
│       └── gateways/
│           ├── AGateway.java       # Basis-Gateway-Klasse
│           ├── PushGateway.java    # Push-Gateway Wrapper
│           ├── PushHTTPGateway.java # Push-Gateway HTTP-Implementierung
│           ├── BulkSmsHttp.java    # (andere Gateways...)
│           └── ...
├── nbproject/                      # NetBeans-Projektdateien
└── dist/                           # Erstellte Anwendung
```

### 7.2 Dependencies

| Library | Version | Zweck |
|---------|---------|-------|
| com.microsoft.sqlserver:mssql-jdbc | 12.2.0.jre11 | SQL Server JDBC |
| com.microsoft.sqlserver:mssql-jdbc_auth | 12.2.0.x64 | Windows-Authentifizierung DLL |
| org.slf4j:slf4j-log4j12 | 1.7.32 | Logging |
| org.jsmpp:jsmpp | 2.3.11 | SMPP-Gateway (SMS-Versand) |
| com.squareup.okhttp3:okhttp | 4.12.0 | HTTP-Client für Push-Gateway |
| com.google.code.gson:gson | 2.11.0 | JSON für Push-Gateway |

### 7.3 Bauen

In NetBeans:
1. Projekt öffnen (File → Open Project → SMSCenterExt-Verzeichnis wählen)
2. Rechtsklick auf Projekt → "Clean and Build" (oder Shift+F11)

Über Kommandozeile:
```bash
cd SMSCenterExt
ant
```

Ivy lädt beim ersten Build alle Dependencies automatisch herunter.

### 7.4 Push-Gateway konfigurieren

Das Push-Gateway wird in der SMSCenter-Oberfläche konfiguriert:

1. SMSCenterExt starten
2. Einstellungen öffnen → Gateway-Konfiguration
3. Neues Gateway hinzufügen, Typ: **Push Notification Gateway**

**Konfigurationsfelder:**

| Feld | Beschreibung | Beispielwert |
|------|-------------|--------------|
| Service URL | URL des push-service Servers | `http://localhost:8080` |
| API Key | Bearer-Token (muss mit `push.api.key` übereinstimmen) | `changeme` |
| Description | Anzeigename | `Push Notification Gateway` |
| Outbound | Ausgehende Nachrichten aktivieren | `true` (Checkbox) |

Diese Werte werden in der SMSServer-Properties-Datei gespeichert als:
```properties
gateway.0.serviceUrl=http://localhost:8080
gateway.0.apiKey=changeme
gateway.0.description=Push Notification Gateway
gateway.0.outbound=yes
```

### 7.5 Empfänger-Zuordnung (Telefonnummer → Spielernummer)

**Das Problem**: SMSCenter kennt Spieler über ihre **Telefonnummer** (z.B. `+436991234567`).
Der push-service kennt Spieler über ihre **Spielernummer** (z.B. `2001`).

**Die Lösung**: Das `PushHTTPGateway` führt eine automatische Rückwärts-Suche durch:

```
SMSCenter will SMS an +436991234567 senden
        ↓
PushHTTPGateway.resolvePlayerId("+436991234567")
        ↓
SQL: SELECT DISTINCT plNr FROM smscenter_phones
     WHERE phone LIKE '%436991234567' OR phone LIKE '%+436991234567'
        ↓
Ergebnis: plNr = "2001"
        ↓
HTTP POST an push-service: {"playerId": "2001", "message": "..."}
```

**Voraussetzung**: In der Tabelle `smscenter_phones` muss die Zuordnung
Spielernummer ↔ Telefonnummer gepflegt sein. Das geschieht über die normale
SMSCenter-Verwaltung.

**Fallback**: Wenn keine Spielernummer gefunden wird, wird die Telefonnummer
als `playerId` verwendet (und eine Warnung geloggt).

---

## 8. SMSCenterExt — Testen

### Voraussetzung

Der push-service muss laufen und mindestens ein Spieler muss im Browser registriert sein.

### Test-Szenario

1. **push-service starten** und Spieler `2001` im Browser registrieren

2. **In der Datenbank** sicherstellen, dass die Zuordnung existiert:
   ```sql
   -- Prüfen, ob die Telefonnummer-Zuordnung vorhanden ist
   SELECT plNr, phone FROM smscenter_phones WHERE plNr = '2001';
   ```

3. **SMSCenterExt starten** mit konfiguriertem Push-Gateway

4. **Nachricht senden**: Im SMSCenter eine Nachricht an den Spieler schicken
   (z.B. Spielergebnis)

5. **Prüfen**:
   - In der SMSCenter-Konsole/Log: Gateway-Meldung "Push sent successfully"
   - Im Browser: Notification und Nachricht in der Liste
   - Im push-service-Log: `INFO SendHandler:117 - Push sent to player 2001: 1/1 devices`

### Manueller Test ohne SMSCenter

Das PushHTTPGateway kann auch ohne SMSCenter getestet werden, indem man direkt
den push-service per curl anspricht (siehe Abschnitt 6.1, Schritt 4).

---

## 9. Zusammenspiel beider Projekte

### 9.1 Gesamtablauf einer Push-Nachricht

```
┌─ Turnierleiter ─────────────────────────────────────────────────────┐
│                                                                      │
│  SMSCenterExt Desktop-App                                           │
│  1. Ergebnis wird eingetragen (z.B. Mueller 3:1 Schmidt)           │
│  2. SMSCenter ruft Database.sendMessage() auf                       │
│  3. Telefonnummer des Spielers wird aus smscenter_phones gelesen    │
│  4. Nachricht wird an PushHTTPGateway übergeben                     │
│                                                                      │
│  PushHTTPGateway                                                     │
│  5. resolvePlayerId(): +436991234567 → "2001"                      │
│  6. HTTP POST an push-service: /api/push/send                       │
│     mit Bearer-Token und {playerId: "2001", message: "..."}        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ HTTP
┌─ push-service (Server) ────────────────────────────────────────────┐
│                                                                      │
│  SendHandler                                                         │
│  7. Bearer-Token prüfen                                             │
│  8. Alle Geräte für Spieler 2001 aus DB laden                      │
│  9. Für jedes Gerät:                                                │
│     a) Nachricht mit AES-128-GCM verschlüsseln (RFC 8291)         │
│     b) VAPID JWT erstellen (RFC 8292)                               │
│     c) HTTP POST an den Push-Endpoint des Browsers                  │
│        (z.B. https://fcm.googleapis.com/fcm/send/...)              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ Web Push Protocol (RFC 8030)
┌─ Browser des Spielers ─────────────────────────────────────────────┐
│                                                                      │
│  Push-Dienst (FCM/Mozilla Push Service)                             │
│  10. Entschlüsselt die Nachricht                                    │
│  11. Leitet sie an den Service Worker weiter                        │
│                                                                      │
│  Service Worker (sw.js)                                              │
│  12. Empfängt Push-Event                                            │
│  13. Zeigt System-Notification an (mit Icon, Text, Vibration)      │
│  14. Leitet Nachricht an offene Tabs weiter                        │
│                                                                      │
│  PWA (app.js)                                                        │
│  15. Empfängt Nachricht vom Service Worker                          │
│  16. Speichert in localStorage (letzte 50 Nachrichten)             │
│  17. Zeigt in der Nachrichtenliste an                               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.2 Integrations-Checkliste

Vor dem produktiven Einsatz prüfen:

- [ ] VAPID-Keys generiert und in `push-service.properties` UND `app.js` eingetragen
- [ ] `push.api.key` in `push-service.properties` geändert (nicht mehr "changeme")
- [ ] Gleicher `apiKey` im SMSCenterExt Push-Gateway konfiguriert
- [ ] `serviceUrl` im SMSCenterExt korrekt (z.B. `http://localhost:8080` oder `https://www.ttm.co.at`)
- [ ] Datenbank-Zugangsdaten in `push-service.properties` korrekt
- [ ] Tabelle `smscenter_phones` ist gepflegt (Spielernummer ↔ Telefonnummer)
- [ ] Firewall-Port freigegeben
- [ ] push-service läuft und ist erreichbar
- [ ] Mindestens ein Spieler hat sich im Browser registriert
- [ ] Test-Push wurde erfolgreich gesendet und empfangen

---

## 10. Deployment auf dem Server (ttm.co.at)

### Voraussetzungen auf dem Server

- Java 17+ (JRE oder JDK)
- MS SQL Server mit der Turnier-Datenbank
- HTTPS-Zertifikat (bereits vorhanden auf ttm.co.at)

### Wichtig: HTTPS ist Pflicht für Web Push

Web Push funktioniert **nur über HTTPS** (Ausnahme: `localhost` für Entwicklung).
Auf dem Produktionsserver muss ein Reverse Proxy (Apache/Nginx/IIS) vor dem
push-service stehen, der HTTPS terminiert.

### Variante: Apache Reverse Proxy

```apache
# In der Apache-Konfiguration (z.B. httpd.conf oder vhost)
<VirtualHost *:443>
    ServerName www.ttm.co.at
    SSLEngine on
    SSLCertificateFile /path/to/cert.pem
    SSLCertificateKeyFile /path/to/key.pem

    # Push-Service weiterleiten
    ProxyPass /push/ http://localhost:8080/push/
    ProxyPassReverse /push/ http://localhost:8080/push/

    ProxyPass /api/push/ http://localhost:8080/api/push/
    ProxyPassReverse /api/push/ http://localhost:8080/api/push/
</VirtualHost>
```

### Variante: IIS Reverse Proxy (URL Rewrite + ARR)

1. URL Rewrite und Application Request Routing (ARR) installieren
2. Inbound Rule erstellen:
   - Pattern: `^(push/.*|api/push/.*)$`
   - Rewrite URL: `http://localhost:8080/{R:0}`

### QR-Code für Spieler

Für den praktischen Einsatz bei Turnieren können QR-Codes generiert werden:

```
URL-Format: https://www.ttm.co.at/push/?player={spielernummer}
Beispiel:   https://www.ttm.co.at/push/?player=2001
```

Der QR-Code kann z.B. auf Spielpläne gedruckt werden, damit Spieler sich
schnell per Smartphone registrieren können.

### push-service als Windows-Dienst

Für den Dauerbetrieb den push-service als Windows-Dienst einrichten (z.B. mit NSSM):

```cmd
nssm install PushService "C:\path\to\java.exe" "-cp" "push-service.jar;lib\*" "com.ttm.push.PushServer"
nssm set PushService AppDirectory "C:\path\to\dist"
nssm start PushService
```

---

## 11. Bekannte Einschränkungen und Hinweise

### Service Worker Verhalten

- **Stoppen ist normal**: Browser stoppen Service Worker nach ca. 30 Sekunden Inaktivität.
  Push-Events wecken sie sofort wieder auf. In `chrome://serviceworker-internals/` wird
  der Status "stopped" angezeigt — das ist kein Fehler.

- **Ein SW pro Scope**: Alle Tabs unter `/push/` teilen denselben Service Worker.
  Deshalb können mehrere Spieler im selben Browser über verschiedene Tabs registriert
  werden, aber die Push-Notifications kommen alle über denselben SW.

### Browser-Kompatibilität

| Browser | Web Push | Hinweise |
|---------|----------|----------|
| Chrome (Desktop) | Ja | Empfohlen, bester Support |
| Firefox (Desktop) | Ja | Funktioniert gut |
| Edge (Desktop) | Ja | Gleiche Engine wie Chrome |
| Chrome (Android) | Ja | Funktioniert |
| Safari (macOS) | Ab 16.4 | Erst seit 2023 |
| Safari (iOS) | Ab 16.4 | Nur als installierte PWA |
| Brave | Teilweise | Kann Probleme bei der Registrierung verursachen |

### Offline-Nachrichten

- Push-Nachrichten werden vom Push-Dienst (FCM/Mozilla) zwischengespeichert
- TTL (Time to Live): 24 Stunden (konfiguriert im `WebPushService.java`)
- Nachrichten, die älter als 24h sind, verfallen
- Beim nächsten Online-Gang des Browsers werden zwischengespeicherte Nachrichten zugestellt

### Datenspeicherung im Browser

| Speicherort | Inhalt | Sichtbar in |
|------------|--------|-------------|
| Push-Subscription | Endpoint + Keys | F12 → Application → Service Workers → Push |
| Local Storage | Nachrichtenliste | F12 → Application → Local Storage → `push_messages_*` |
| Service Worker | Registrierung | `chrome://serviceworker-internals/` |

### Sicherheitshinweise

- **`push.api.key`**: Unbedingt ändern! Jeder der diesen Key kennt, kann
  Push-Nachrichten an alle registrierten Spieler senden.
- **VAPID Private Key**: Niemals veröffentlichen. Liegt nur auf dem Server.
- **HTTPS**: In Produktion zwingend erforderlich (nicht nur empfohlen).
- **CORS**: Die API-Endpoints erlauben derzeit alle Origins (`Access-Control-Allow-Origin: *`).
  Für Produktion auf die eigene Domain einschränken.

---

## 12. Technische Details (Kryptografie)

Dieser Abschnitt ist als Referenz gedacht und für den normalen Betrieb nicht relevant.

### Verwendete Standards

| RFC | Titel | Wofür |
|-----|-------|-------|
| RFC 8030 | Generic Event Delivery Using HTTP Push | Web Push Protokoll |
| RFC 8291 | Message Encryption for Web Push | Nachrichten-Verschlüsselung |
| RFC 8292 | Voluntary Application Server Identification (VAPID) | Server-Identifikation |
| RFC 8188 | Encrypted Content-Encoding for HTTP | aes128gcm Format |
| RFC 5869 | HMAC-based Key Derivation Function (HKDF) | Schlüsselableitung |

### Verschlüsselungsablauf

```
1. Browser erstellt bei Registrierung ein P-256 Schlüsselpaar:
   - Public Key (p256dh): 65 Bytes, wird an den Server geschickt
   - Private Key: bleibt im Browser
   - Auth Secret: 16 Bytes Zufallsdaten, wird an den Server geschickt

2. Server verschlüsselt jede Nachricht individuell:
   a) Erzeugt ephemeres P-256 Schlüsselpaar (einmalig pro Nachricht)
   b) ECDH Key Agreement: server_private × client_public → shared_secret
   c) HKDF-Extract(auth_secret, shared_secret) → IKM
   d) HKDF-Expand(IKM, info_string) → PRK (32 Bytes)
   e) Leitet ab: Content Encryption Key (16 Bytes) + Nonce (12 Bytes)
   f) AES-128-GCM Verschlüsselung der Nachricht
   g) Baut aes128gcm-Header:
      [16 Bytes Salt] [4 Bytes Record Size] [1 Byte KeyID Len]
      [65 Bytes ephemeral Public Key] [Encrypted Data]

3. VAPID JWT (für Authentifizierung beim Push-Dienst):
   - Header: {"typ":"JWT","alg":"ES256"}
   - Payload: {"aud":"https://fcm...","exp":...,"sub":"mailto:admin@ttm.co.at"}
   - Signatur: ES256 (ECDSA mit SHA-256 auf P-256)
   - Authorization Header: "vapid t=<JWT>,k=<public_key>"
```

### JDK-Klassen (keine externen Libraries)

| JDK-Klasse | Verwendung |
|------------|------------|
| `java.security.KeyPairGenerator` | P-256 Schlüsselpaar erzeugen |
| `javax.crypto.KeyAgreement` | ECDH Shared Secret |
| `javax.crypto.Mac` (HmacSHA256) | HKDF Extract + Expand |
| `javax.crypto.Cipher` (AES/GCM/NoPadding) | Nachricht verschlüsseln |
| `java.security.Signature` (SHA256withECDSA) | VAPID JWT signieren |
| `java.net.http.HttpClient` | Push an FCM/Mozilla senden |
| `com.sun.net.httpserver.HttpServer` | HTTP-Server (JDK built-in) |
