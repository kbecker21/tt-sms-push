# Release Notes — v7.2 (2026-03-28): Player-Spalte im Outgoing-Panel

## Neue Spalte "Player" in der Outgoing-Tabelle

Im Panel/Tab "Outgoing" wird zwischen "Status" und "Receiver" die neue Spalte **"Player"**
angezeigt. Sie enthält den Wert aus `smsserver_out.plNr` und zeigt direkt, welchem Spieler
die Nachricht zugeordnet ist.

**Spalten-Layout vorher**: Row | ID | Status | Receiver | Text | Date | Ref | Gateway
**Spalten-Layout neu**: Row | ID | Status | **Player** | Receiver | Text | Date | Ref | Gateway

### Geänderte Dateien

| Datei | Änderung |
|-------|----------|
| `Database.java` (smscenter/database) | `getOutgoingMessages()`: SQL SELECT um `plNr` erweitert, Object-Array von 8 auf 9 Elemente, `rows`-Array für Sortierung erweitert |
| `OutgoingPanel.java` | Tabellenmodell um "Player"-Spalte erweitert, alle Spaltenindizes ab Position 3 um +1 verschoben (betrifft `updateStatus()`, `outgoingTableMouseClicked()`, `queryButtonActionPerformed()`) |

### Hinweis zur Index-Verschiebung

Da die neue Spalte **zwischen** bestehenden Spalten eingefügt wird (nicht am Ende),
mussten alle hartcodierten Spaltenindizes >= 3 in `OutgoingPanel.java` angepasst werden:

- `updateStatus()`: ref `row[6]→row[7]`, gateway `row[7]→row[8]`
- `outgoingTableMouseClicked()`: receiver `3→4`, text `4→5`, date `5→6`, ref `6→7`
- `queryButtonActionPerformed()`: ref `6→7`
- Spalten 0 (Row), 1 (ID), 2 (Status) bleiben unverändert

---

# Release Notes — v7.1 (2026-03-28): Bugfix Kompilierfehler + SQL-Spaltenreihenfolge

## Bugfixes

### Kompilierfehler in interfaces/Database.java behoben

`msg.getMessageId()` gibt `long` zurück, nicht `String`. Der Aufruf
`PushHTTPGateway.setPlayerNr(msg.getMessageId(), ...)` schlug fehl mit:
> Inkompatible Typen: long kann nicht in String konvertiert werden

**Fix**: `PushHTTPGateway.playerNrCache` von `ConcurrentHashMap<String, String>`
auf `ConcurrentHashMap<Long, String>` geändert. Die Methode `setPlayerNr()` akzeptiert
jetzt `long messageId` statt `String messageId`.

### SQL-Schema: plNr-Spalte vor recipient verschoben

In `smsserver_out.sql` steht `plNr` jetzt direkt nach `type` und vor `recipient`
(wie von Michael gewünscht, bessere Lesbarkeit).

---

# Release Notes — v7 (2026-03-28): Spielernummer direkt statt Rückwärtssuche

## Kernänderung: Spielernummer (plNr) direkt in smsserver_out gespeichert

### Problem: Rückwärtssuche liefert falschen Spieler (Kritisch)

Das `PushHTTPGateway` führte eine Rückwärtssuche durch, um von der Telefonnummer
auf die Spielernummer zu schliessen:
```sql
SELECT DISTINCT plNr FROM smscenter_phones
WHERE phone LIKE '%436991234567' OR phone LIKE '%+436991234567'
```

Wenn mehrere Spieler dieselbe Telefonnummer haben (z.B. Vater meldet zwei Kinder an),
liefert diese Abfrage den **ersten** Treffer statt den richtigen Spieler. Die Push-Nachricht
wurde an den falschen Spieler zugestellt.

### Lösung: SMSCenter-Kern speichert plNr direkt

Die Spielernummer wird jetzt bereits beim Einfügen in die Tabelle `smsserver_out` gespeichert
und direkt an den `PushHTTPGateway` weitergereicht. Die Rückwärtssuche entfällt als primärer
Mechanismus und dient nur noch als Fallback für Altdaten.

**Datenfluss (neu)**:
```
SMSCenter sendMessage("42", text)
  → plNr = 42 % 10000 = 42
  → INSERT INTO smsserver_out (recipient, text, ..., plNr) VALUES('+43...', text, ..., 42)

getMessagesToSend()
  → SELECT ..., plNr FROM smsserver_out WHERE status = 'U'
  → PushHTTPGateway.setPlayerNr(messageId, "42")

PushHTTPGateway.sendMessage(msg)
  → playerId = playerNrCache.remove(messageId)  // "42" — direkt, keine DB-Abfrage
  → POST /api/push/send {playerId: "42", message: "..."}
```

**Änderungen im Detail**:

1. **`smsserver_out`-Tabelle** — Neue Spalte `plNr` (int, nullable):
   ```sql
   ALTER TABLE smsserver_out ADD plNr int NULL DEFAULT(NULL);
   ```

2. **`Database.java`** (SMSCenter-Kern) — `sendMessage()` berechnet `plNr % 10000` und
   `sendMessageToNumber()` speichert die plNr im INSERT.

3. **`interfaces/Database.java`** — `getMessagesToSend()` liest `plNr` aus dem ResultSet
   und übergibt sie über einen statischen Cache (`PushHTTPGateway.setPlayerNr()`) an das Gateway.

4. **`PushHTTPGateway.java`** — `sendMessage()` liest `plNr` aus dem Cache (`playerNrCache.remove()`).
   Nur wenn keine plNr im Cache vorhanden ist (Altdaten, direkte Telefonnummer-Sends),
   wird die bisherige Rückwärtssuche als Fallback verwendet.

### Warum plNr % 10000?

Organisatorische Gründe: Die Spielernummer wird 4-stellig gespeichert. `plNr mod 10000`
stellt sicher, dass die Nummer unabhängig von Präfixen korrekt abgelegt wird.

### Warum kein neues Feld in OutboundMessage?

Die Klasse `OutboundMessage` ist Teil der kompilierten Bibliothek `smslib-v3.jar` und
kann nicht direkt erweitert werden. Stattdessen wird ein statischer `ConcurrentHashMap`-Cache
in `PushHTTPGateway` als Transfer-Mechanismus verwendet (`messageId → plNr`). Der Cache
verwendet `remove()` statt `get()`, um Memory-Leaks zu vermeiden.

## Verbesserungen

### WebPushService: Error-Handling für Endpoint-URL und HTTP-Aufruf

Michaels Anmerkungen zu `WebPushService.sendPush()`:

1. **`URI.create(device.endpoint)`**: Wird jetzt in einem try/catch-Block ausgeführt.
   Bei einer ungültigen URL (`IllegalArgumentException`) wird ein Fehler geloggt und
   `-1` zurückgegeben, statt die gesamte Methode mit einer Exception abzubrechen.

2. **`httpClient.send()`**: Wird jetzt explizit in einem try/catch für `IOException`
   ausgeführt. Der Fehler wird mit der Endpoint-URL geloggt, bevor die Exception
   weitergeworfen wird — so ist im Log erkennbar, welcher Endpoint den Fehler verursacht hat.

3. **Kein `synchronized` nötig**: Der JDK `HttpClient` ist thread-safe (Javadoc:
   "An HttpClient can be used to send multiple requests"). Im push-service werden
   Requests pro `SendHandler`-Aufruf sequentiell abgearbeitet. Anders als beim
   `PushHTTPGateway` (wo SMSLib-Threads parallel senden) ist hier kein
   `synchronized`-Block erforderlich.

### IndexedDB — weiterhin benötigt

Michaels Beobachtung: "Die Push-Nachrichten stehen nun im LocalStorage des Browsers,
aber die IndexedDB ist jetzt leer! Komisch. Vielleicht wird sie nicht mehr gebraucht?"

**Antwort**: Die IndexedDB wird **weiterhin benötigt** als Offline-Puffer. Sie ist leer,
weil sie korrekt funktioniert:

- Wenn ein Tab für den Spieler geöffnet ist, werden Nachrichten direkt per `postMessage`
  an den Tab geleitet und in localStorage gespeichert. IndexedDB wird nicht beschrieben.
- Nur wenn **kein** Tab für den betroffenen Spieler offen ist, speichert der Service Worker
  die Nachricht in IndexedDB. Beim nächsten Tab-Öffnen werden die Nachrichten aus IndexedDB
  in localStorage übernommen und aus IndexedDB gelöscht.
- Eine leere IndexedDB bedeutet: Alle Nachrichten wurden erfolgreich an einen offenen Tab
  zugestellt — das ist der Normalfall.

## Geänderte Dateien (Zusammenfassung)

| Datei | Änderung |
|-------|----------|
| `smsserver_out.sql` | Neue Spalte `plNr` (int, nullable) + Migration-Kommentar |
| `Database.java` (smscenter/database) | `sendMessage()`: plNr % 10000 berechnen; `sendMessageToNumber()`: plNr im INSERT speichern |
| `Database.java` (smsserver/interfaces) | `getMessagesToSend()`: plNr aus ResultSet lesen, `PushHTTPGateway.setPlayerNr()` aufrufen |
| `PushHTTPGateway.java` | Statischer `playerNrCache` (ConcurrentHashMap), `sendMessage()` nutzt Cache statt Rückwärtssuche |
| `WebPushService.java` | try/catch für `URI.create()` + `httpClient.send()` |

## Dokumentation aktualisiert

- `DOKUMENTATION.md` — Abschnitte 7.5 (Empfänger-Zuordnung: neuer plNr-Flow), 7.6 (SMSLib-Integration),
  9.1 (Gesamtablauf), 11 (Bekannte Einschränkungen, IndexedDB-Erklärung, DB-Migration) aktualisiert
- `RELEASE_NOTES.md` — Diesen Eintrag hinzugefügt

## Hinweise zum Update

1. **Datenbank-Migration**: Vor dem ersten Start muss die neue Spalte angelegt werden:
   ```sql
   ALTER TABLE smsserver_out ADD plNr int NULL DEFAULT(NULL);
   ```
2. **SMSCenterExt**: Vollständigen `Clean and Build` in NetBeans ausführen (Shift+F11)
3. **push-service**: Neu bauen und deployen (`ant compile && ant jar && ant dist`)
4. **Browser**: Keine Änderungen nötig
5. **Bestehende Nachrichten**: Nachrichten, die bereits in `smsserver_out` stehen (ohne plNr),
   werden weiterhin über die Rückwärtssuche zugestellt (Fallback)

---

# Release Notes — v6 (2026-03-27): Doppelte Nachrichten-Zustellung behoben

## Bugfixes

### Jede Nachricht wurde mehrfach zugestellt (Kritisch)

Beim Empfang einer neuen Nachricht im Browser wurde die vorherige Nachricht erneut
angezeigt. Jede Nachricht erschien mehrmals in der Nachrichtenliste.

**Ursache 1 — Mehrfache Device-Registrierungen** (Hauptursache):
`Database.registerDevice()` machte ein Upsert auf `player_id + endpoint`. Wenn sich
der Browser-Endpoint änderte (nach SW-Update, Browser-Neustart, erneute Push-Subscription),
blieb der alte Eintrag in der Datenbank erhalten. `SendHandler.handle()` sendete dann den
Web Push an ALLE registrierten Endpoints für den Spieler → der Browser empfing die
gleiche Nachricht mehrfach (einmal pro Endpoint).

**Ursache 2 — Doppelte Speicherung (IndexedDB + localStorage)**:
Der Service Worker speicherte jede eingehende Push-Nachricht gleichzeitig in IndexedDB
UND leitete sie per `postMessage` an offene Tabs weiter. Bei Seitenneuladen wurden die
IndexedDB-Nachrichten als "verpasst" erneut geladen, und die zeitbasierte Deduplizierung
versagte wegen unterschiedlicher Zeitstempel-Formate.

**Ursache 3 — Keine clientseitige Deduplizierung**:
Der `push-message`-Handler in `app.js` fügte jede empfangene Nachricht blind in
localStorage ein, ohne zu prüfen ob sie bereits vorhanden war. Bei Retries
(z.B. SMSCenter-Queue bei Timeout) oder Debugger-Pausen wurde dieselbe Nachricht
mehrfach gespeichert und angezeigt.

**Ursache 4 — Multi-Tab Race Condition**:
Bei mehreren offenen Tabs liefert `clients.matchAll()` im Service Worker alle Tabs.
Jeder Tab empfängt einen `postMessage` und schreibt in den gemeinsamen localStorage.
Der zweite Tab sieht die Schreiboperation des ersten und fügt die Nachricht erneut hinzu.

**Ursache 5 — SMSCenter-Retry mit neuer messageId**:
Bei einem Timeout des push-service (z.B. bei Debugger-Pause) gibt `PushHTTPGateway.sendMessage()`
`false` zurück. SMSLib behält die Nachricht in der Queue und sendet sie erneut. Jeder neue
HTTP-Request erzeugte bisher eine neue zufällige `messageId` im `SendHandler`, sodass der
Browser den Retry nicht als Duplikat erkennen konnte.

**Ursache 6 — Nachrichten anderer Spieler im eigenen Tab sichtbar**:
Jeder offene Tab speicherte Nachrichten für **alle** Spieler in localStorage (Cross-Player-
Speicherung aus v3). Bei fehlender oder leerer `playerId` im Push-Payload fiel
`msgPlayerId` auf den eigenen Spieler zurück → fremde Nachricht landete im eigenen Storage.

**Lösung (siebenstufig, über alle Schichten)**:

1. **Database.java** — Registrierung bereinigt alte Einträge:
   Bei Registrierung werden alle bestehenden Einträge für den Spieler gelöscht,
   bevor der neue eingefügt wird → max. 1 Device pro Spieler → kein Mehrfachversand.

2. **PushHTTPGateway.java** — Deterministische messageId vom Sender:
   Erzeugt eine `messageId` per `UUID.nameUUIDFromBytes(playerId + text)`. Da der
   Hash deterministisch ist, erzeugen SMSLib-Retries die **identische** ID.
   Der Browser erkennt Retries und ignoriert sie.

3. **SendHandler.java** — Übernimmt messageId vom Sender:
   Verwendet die `messageId` aus dem HTTP-Request (von PushHTTPGateway). Nur wenn
   keine mitgeliefert wird (z.B. bei direktem curl-Aufruf), erzeugt er eine zufällige.

4. **sw.js** — Spieler-bezogene Speicherung + Notification-Dedup:
   - postMessage an alle offenen Tabs (jeder Tab filtert selbst)
   - Kein Tab für den betroffenen Spieler offen → IndexedDB-Speicherung
   - Notification-Tag basiert auf `messageId` → doppelte OS-Notifications werden
     vom Browser automatisch ersetzt statt gestapelt.

5. **app.js** — Strikte Spieler-Isolation:
   Der `push-message`-Handler verarbeitet **nur** Nachrichten für den eigenen Spieler.
   Nachrichten anderer Spieler werden ignoriert (`msgPlayerId !== playerId → return`).
   Die Cross-Player-Speicherung aus v3 wurde entfernt.

6. **app.js** — messageId-Deduplizierung im Client:
   Beide Message-Handler (`push-message` und `missed-messages`) prüfen vor dem
   Speichern, ob die `messageId` bereits in localStorage existiert → Duplikate
   von Multi-Tab, Retries oder IndexedDB-Merge werden erkannt und ignoriert.

7. **app.js** — playerId im localStorage-Eintrag:
   Jeder Nachrichteneintrag speichert jetzt auch die `playerId`, damit im
   DevTools-Inspektor sofort erkennbar ist, welchem Spieler die Nachricht gehört.

## Geänderte Dateien (Zusammenfassung)

| Datei | Änderung |
|-------|----------|
| `PushHTTPGateway.java` | Deterministische `messageId` (`UUID.nameUUIDFromBytes`) im HTTP-Request, stabil über Retries |
| `Database.java` | `registerDevice()`: DELETE all + INSERT — garantiert 1 Device pro Spieler |
| `SendHandler.java` | Liest `messageId` aus Request (Fallback: `UUID.randomUUID()`) |
| `sw.js` | Spieler-bezogene IndexedDB-Speicherung (nur wenn kein Tab für den Spieler offen), Notification-Tag per `messageId` |
| `app.js` | Strikte Spieler-Isolation (nur eigene Nachrichten), Dedup per `messageId`, `playerId` im localStorage-Eintrag |

## Dokumentation aktualisiert

- `DOKUMENTATION.md` — Abschnitte 5.2 (Service Worker), 7.6 (Push-Payload), 9.1 (Gesamtablauf), 11 (Offline-Nachrichten, Service Worker Verhalten, Registrierung) aktualisiert
- `RELEASE_NOTES.md` — Diesen Eintrag hinzugefügt

## Hinweise zum Update

1. **push-service**: Neu bauen und deployen (`ant compile && ant jar && ant dist`)
2. **SMSCenterExt**: Vollständigen `Clean and Build` in NetBeans ausführen (Shift+F11)
3. **Browser**: Spieler müssen **Ctrl+F5** drücken (oder den Service Worker in den DevTools
   deregistrieren), damit die aktualisierte `sw.js` geladen wird
4. **Wichtig**: Nach dem Update sollte sich jeder Spieler einmal **neu registrieren**
   (Abmelden → erneut Aktivieren), damit alte Device-Einträge in der DB bereinigt werden

## Bekannte Einschränkung

Die `messageId` wird deterministisch aus `playerId + Nachrichtentext` erzeugt. Wenn exakt
derselbe Text an denselben Spieler nochmals gesendet wird (z.B. "Tisch 3 bitte" zweimal),
erkennt der Browser die zweite Nachricht als Duplikat und ignoriert sie. Workaround:
Nachrichtentext leicht variieren (z.B. "Tisch 3 bitte (2)").

---

# Release Notes — v5 (2026-03-26): Fehler und Korrekturen in SMSCenterExt

## Bugfixes

### msg.setRefNo() statt msg.setDispatchDate() in PushHTTPGateway (Fehler)

In `PushHTTPGateway.sendMessage()` wurde der Timestamp aus der push-service-Response
fälschlicherweise in `msg.refNo` gespeichert statt in `msg.dispatchDate`.

**Lösung**: `msg.setRefNo(timestamp)` ersetzt durch `msg.setDispatchDate(parsedDate)`.
Der Timestamp wird jetzt als `java.util.Date` geparst. Wenn kein Timestamp in der
Response vorhanden ist oder das Parsen fehlschlägt, wird die aktuelle lokale Zeit verwendet.
`msg.refNo` wird jetzt wie beim BulkSmsHTTPGateway mit einem aufsteigenden Zähler gesetzt.

**Geänderte Datei**: `PushHTTPGateway.java`

### Ungültige Service URL führt zu unbehandelter Exception (Fehler)

Wenn die Service URL ungültig war (z.B. ohne Schema), warf `Request.Builder.url()`
eine `IllegalArgumentException`, die nicht gefangen wurde.

**Lösung**: Der `Request.Builder`-Aufruf ist jetzt in einen try/catch-Block gepackt.
Bei einer ungültigen URL wird eine Fehlermeldung geloggt und `false` zurückgegeben.

**Geänderte Datei**: `PushHTTPGateway.java`

### Timestamp-Format zu ungenau (Verbesserung)

Der `SendHandler` im push-service gab den Timestamp im Format `"EEE HH:mm z"` zurück
(z.B. `"Fri 14:30 CET"`). Dieses Format enthielt kein Datum und keine Sekunden/Millisekunden,
was für `msg.dispatchDate` nicht ausreicht.

**Lösung**: Neues Format `"yyyy-MM-dd HH:mm:ss.SSS Z"` (z.B. `"2026-03-26 14:30:05.123 +0100"`).
Das Format wird von `PushHTTPGateway` beim Setzen von `msg.dispatchDate` korrekt geparst.

**Geänderte Datei**: `SendHandler.java`

## Verbesserungen

### Synchronized HTTP-Aufruf in PushHTTPGateway

Der HTTP POST in `PushHTTPGateway.sendMessage()` wird jetzt `synchronized` über ein
`SYNC_Commander`-Objekt ausgeführt, analog zum BulkSmsHTTPGateway. Dies verhindert
Race Conditions bei gleichzeitigem Versand mehrerer Nachrichten.

**Geänderte Datei**: `PushHTTPGateway.java`

### Automatische URL-Schema-Ergänzung im Settings Panel

Beim Speichern der Gateway-Konfiguration wird die Service URL automatisch um das Schema
ergänzt, falls es fehlt:
- `localhost`, `127.x.x.x`, `::1` → `http://` vorangestellt
- Alle anderen Adressen → `https://` vorangestellt
- Bereits vorhandene Schemas werden nicht verändert

**Geänderte Datei**: `PushGatewaySettingsPanel.java`

## Geänderte Dateien (Zusammenfassung)

| Datei | Änderung |
|-------|----------|
| `PushHTTPGateway.java` | try/catch für URL-Validierung, `setDispatchDate()` statt `setRefNo()`, Timestamp-Parsing, Fallback auf aktuelle Zeit, synchronized HTTP-Aufruf, refCount-Zähler |
| `PushGatewaySettingsPanel.java` | `normalizeServiceUrl()` — automatische Schema-Ergänzung (http/https) beim Speichern |
| `SendHandler.java` | Timestamp-Format geändert auf `yyyy-MM-dd HH:mm:ss.SSS Z` |

## Dokumentation aktualisiert

- `DOKUMENTATION.md` — Abschnitte 4.2 (API-Response Timestamp-Format), 7.4 (URL-Normalisierung), 7.6 (Nachrichtenversand-Ablauf), 11 (Troubleshooting: ungültige URL) aktualisiert
- `RELEASE_NOTES.md` — Diesen Eintrag hinzugefügt

## Hinweise zum Update

1. **SMSCenterExt**: Vollständigen `Clean and Build` in NetBeans ausführen (Shift+F11)
2. **push-service**: Neu bauen und deployen (`ant compile && ant jar && ant dist`)
3. **Browser**: Keine Änderungen nötig

---

# Release Notes — v4 (2026-03-25): SMSCenterExt ↔ push-service Integration

## Bugfixes

### SMSLib Outbound-Routing für PushHTTPGateway (Kritisch)

Nachrichten, die im SMSCenter erstellt und in `smsserver_out` mit Status `U` (Unsent) gespeichert
wurden, wurden nicht an den push-service weitergeleitet. Die Methode `PushHTTPGateway.sendMessage()`
wurde nie aufgerufen.

**Ursache**: SMSLib v3 verwendet ein internes Bitfeld `attributes` in der Basisklasse `AGateway`,
das steuert, ob `setOutbound(true)` tatsächlich wirkt:
```java
// AGateway.setOutbound() — vereinfacht:
public void setOutbound(boolean value) {
    if ((this.attributes & 1) != 0) {   // Bit 0 = Outbound-Fähigkeit
        this.outbound = value;
    }
    // Wenn Bit 0 nicht gesetzt → Aufruf wird STILL IGNORIERT
}
```
Der Konstruktor `AGateway(String id)` setzt `attributes = 0`. Dadurch war `setOutbound(true)`
in `PushGateway.create()` wirkungslos, das Gateway blieb intern als `outbound = false` markiert,
und der SMSLib-Router (`Router.preroute()`) fand kein passendes Gateway für den Versand.

**Ablaufkette des Fehlers**:
```
OutboundPollingThread → sendMessages() → Router.preroute()
    → prüft für jedes Gateway: isOutbound() && getStatus() == STARTED
    → PushHTTPGateway.isOutbound() gibt false zurück (wegen attributes-Bug)
    → Kein Gateway-Kandidat → routeMessage() gibt null zurück
    → Nachricht bleibt mit Status "U" in der Datenbank
```

**Lösung**: Im Konstruktor von `PushHTTPGateway` wird jetzt `setAttributes(getAttributes() | 1)`
aufgerufen, um das Outbound-Bit explizit zu setzen, bevor `setOutbound(true)` aufgerufen wird.

**Geänderte Datei**: `SMSCenterExt/src/smscenter/smsserver/gateways/PushHTTPGateway.java`

### Kotlin-Laufzeitabhängigkeit für OkHttp (Kritisch)

Beim Start des SMS-Servers trat folgender Fehler auf:
```
java.lang.NoClassDefFoundError: kotlin/jvm/internal/Intrinsics
    at okhttp3.MediaType$Companion.get(MediaType.kt)
    at smscenter.smsserver.gateways.PushHTTPGateway.<clinit>(PushHTTPGateway.java:34)
```

**Ursache**: OkHttp 4.x ist in Kotlin geschrieben und benötigt `kotlin-stdlib` als
Laufzeitabhängigkeit. Ivy löst diese transitiv auf, die JARs (`kotlin-stdlib-1.9.10.jar` etc.)
landen in `lib/`, werden aber bei einem `Clean and Build` in NetBeans automatisch nach
`dist/lib/` kopiert — vorausgesetzt der Build wird vollständig ausgeführt.

**Lösung**: Ein vollständiger `Clean and Build` in NetBeans (Shift+F11) oder `ant clean jar`
stellt sicher, dass alle transitiven Abhängigkeiten korrekt in `dist/lib/` kopiert werden.
Die Kotlin-JARs sind bereits in `lib/` vorhanden und erfordern keine manuellen Schritte.

## Geänderte Dateien (Zusammenfassung)

| Datei | Änderung |
|-------|----------|
| `PushHTTPGateway.java` | `setAttributes(getAttributes() \| 1)` im Konstruktor — aktiviert Outbound-Bit |

## Dokumentation aktualisiert

- `DOKUMENTATION.md` — Abschnitte 7.2 (Dependencies), 7.6 (Technischer Hintergrund: SMSLib-Integration), 8 (Testen), 9.2 (Integrations-Checkliste), 11 (Troubleshooting) erweitert
- `RELEASE_NOTES.md` — Diesen Eintrag hinzugefügt

## Hinweise zum Update

1. **SMSCenterExt**: Vollständigen `Clean and Build` in NetBeans ausführen (Shift+F11)
2. **push-service**: Keine Änderungen nötig
3. **Browser**: Keine Änderungen nötig

---

# Release Notes — push-service v3 (2026-03-23)

## Bugfixes

### Verpasste Nachrichten bei Multi-Player-Szenario (Kritisch)
Wenn mehrere Spieler im selben Browser registriert waren und ein Spieler-Tab geschlossen
wurde, gingen Nachrichten für diesen Spieler verloren. Beim erneuten Öffnen des Tabs
war die Nachrichtenliste leer, obwohl Nachrichten gesendet wurden.

**Ursache**: Jeder Tab speicherte nur Nachrichten für "seinen" Spieler und ignorierte
Nachrichten für andere Spieler. Wenn kein passender Tab offen war, ging die Nachricht
verloren, weil der Service Worker sie nicht zwischenspeicherte.

**Lösung (zweistufig)**:
1. Jeder offene Tab speichert jetzt Nachrichten für **alle** Spieler in localStorage
   (nicht nur für den eigenen). So geht keine Nachricht verloren, solange mindestens
   ein Tab offen ist.
2. Der Service Worker speichert zusätzlich jede eingehende Nachricht in IndexedDB.
   Beim Öffnen eines Tabs werden verpasste Nachrichten aus IndexedDB abgerufen,
   in die Nachrichtenliste gemergt und aus IndexedDB gelöscht. Dies funktioniert
   auch wenn gar kein Tab geöffnet war.

**Geänderte Dateien**: `sw.js`, `app.js`

## Geänderte Dateien (Zusammenfassung)

| Datei | Änderung |
|-------|----------|
| `sw.js` | IndexedDB-Funktionen für Offline-Nachrichtenpuffer, Speicherung bei jedem Push-Event, neuer `message`-Listener für Tab-Anfragen |
| `app.js` | Nachrichten für alle Spieler in localStorage speichern, verpasste Nachrichten beim Tab-Öffnen vom SW abrufen, IndexedDB-Cleanup beim Löschen |

## Dokumentation aktualisiert

- `DOKUMENTATION.md` — Abschnitte 5.2 (Service Worker), 9.1 (Gesamtablauf), 11 (Offline-Nachrichten, Datenspeicherung) aktualisiert

## Hinweise zum Update

**Wichtig**: Nach dem Update müssen Spieler im Browser **Ctrl+F5** drücken
(oder den Service Worker in den DevTools deregistrieren), damit die aktualisierte
`sw.js` geladen wird. Die IndexedDB wird beim ersten Push automatisch angelegt.

---

# Release Notes — push-service v2 (2026-03-22)

## Bugfixes

### Nachrichten-Zustellung pro Spieler (Kritisch)
Wenn mehrere Spieler im selben Browser (verschiedene Tabs) registriert waren,
wurde eine Nachricht an Spieler A auch im Tab von Spieler B angezeigt.

**Lösung**: Der Server sendet jetzt die `playerId` im Push-Payload mit.
Der Service Worker leitet die `playerId` an alle Tabs weiter, und jeder Tab
filtert nur die Nachrichten für "seinen" Spieler.

**Geänderte Dateien**: `SendHandler.java`, `sw.js`, `app.js`

### Re-Registrierung nach Abmelden (Kritisch)
Nach dem Klick auf "Abmelden" und erneutem Klick auf "Aktivieren" passierte nichts.

**Ursache**: Der Click-Handler wurde beim Seitenaufbau nur einmal gesetzt und nach
dem Abmelden nicht neu angehängt.

**Lösung**: Neue Funktion `attachSubscribeHandler()` in `app.js`, die den Button-Handler
jederzeit (neu) setzen kann. Nach dem Abmelden wird der Handler automatisch re-aktiviert.

**Geänderte Datei**: `app.js`

### Stille Auto-Registrierung neuer Spieler
Wenn bereits ein Spieler im Browser registriert war und ein zweiter Spieler-Tab
geöffnet wurde, wurde der neue Spieler automatisch ohne Klick registriert.

**Lösung**: Der "Aktivieren"-Button wird jetzt immer angezeigt, wenn der Server den
Spieler nicht kennt. Eine explizite Bestätigung durch den Benutzer ist erforderlich.

**Geänderte Datei**: `app.js`

## Neue Features

### Spielernummer-Eingabefeld
Die URL `http://localhost:8080/push/` (ohne `?player=...` Parameter) zeigt jetzt
ein Eingabefeld für die Spielernummer. Nach Eingabe und Klick auf "Weiter" wird
auf die URL mit Spielernummer weitergeleitet.

**Vorteil**: Ein universeller QR-Code kann in der Halle aufgehängt werden, den alle
Spieler scannen. Jeder gibt dann seine eigene Nummer ein.

**Geänderte Dateien**: `index.html`, `app.js`, `style.css`

### Spielernummer in System-Notifications
Windows-/System-Notifications zeigen jetzt den Spieler im Titel an:
`"TTM - TEST01"` statt nur `"TTM"`. So ist sofort erkennbar, für wen die
Nachricht bestimmt ist — besonders nützlich bei mehreren Spielern auf einem Gerät.

**Geänderte Datei**: `sw.js`

### Übersetzungen ausgelagert (i18n.json)
Die Sprachübersetzungen (DE, EN, ES, FR, JA) wurden aus `app.js` in eine
separate Datei `web/push/i18n.json` ausgelagert. Texte können jetzt ohne
Code-Änderung angepasst werden, und neue Sprachen sind einfach hinzuzufügen.

**Neue Datei**: `i18n.json`
**Geänderte Datei**: `app.js`

## Geänderte Dateien (Zusammenfassung)

| Datei | Änderung |
|-------|----------|
| `SendHandler.java` | Push-Payload enthält jetzt `{playerId, message}` als JSON |
| `sw.js` | Parst `playerId`, zeigt Spieler im Notification-Titel, filtert bei Klick |
| `app.js` | Nachrichten-Filter, `attachSubscribeHandler()`, Player-Input, i18n extern |
| `index.html` | Neues Eingabefeld für Spielernummer |
| `style.css` | Styling für Eingabefeld |
| `i18n.json` | **Neu** — ausgelagerte Übersetzungen |

## Dokumentation aktualisiert

- `DOKUMENTATION.md` — Alle betroffenen Abschnitte aktualisiert (Registrierungs-Ablauf, Service Worker, Mehrsprachigkeit, Tests, Troubleshooting, QR-Codes)
- `push-service/README.md` — PowerShell-Beispiele, Player-Input, universeller QR-Code
- `Meine Anleitung zum Testen.txt` — PowerShell-kompatible curl-Befehle, Mehrspieler-Test

## Hinweise zum Update

Nach dem Update müssen alle Dateien neu gebaut und deployed werden:
```bash
cd push-service
ant compile
ant jar
ant dist
```

**Wichtig**: Nach dem Update sollten Spieler im Browser einmalig **Ctrl+F5** drücken
(oder den Service Worker in den DevTools deregistrieren), damit die aktualisierte
`sw.js` und `app.js` geladen werden.
