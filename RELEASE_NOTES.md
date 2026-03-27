# Release Notes — v6 (2026-03-27): Doppelte Nachrichten-Zustellung behoben

## Bugfixes

### Jede Nachricht wurde doppelt zugestellt (Kritisch)

Beim Empfang einer neuen Nachricht im Browser wurde die vorherige Nachricht erneut
angezeigt. Jede Nachricht erschien insgesamt zweimal in der Nachrichtenliste.

**Ursache**: Der Service Worker speicherte jede eingehende Push-Nachricht **gleichzeitig**
in IndexedDB (Offline-Puffer) UND leitete sie per `postMessage` an offene Tabs weiter.
Dadurch existierte jede Nachricht in zwei Speichern (IndexedDB + localStorage). Bei
einem Seitenneuladen oder Tab-Wechsel wurden die IndexedDB-Nachrichten als "verpasste
Nachrichten" erneut geladen. Die zeitbasierte Deduplizierung (`time + '|' + text`)
konnte diese nicht erkennen, weil die Zeitstempel aus unterschiedlichen Quellen stammten
(IndexedDB: `toISOString()` im Service Worker, localStorage: `toLocaleTimeString()` im Tab).

**Lösung (zweistufig)**:
1. **sw.js**: Der Service Worker prüft jetzt **zuerst**, ob Tabs geöffnet sind:
   - Tabs offen → Nachricht wird nur per `postMessage` weitergeleitet (KEIN IndexedDB)
   - Kein Tab offen → Nachricht wird nur in IndexedDB gespeichert (für späteres Abrufen)
   Die doppelte Speicherung ist damit ausgeschlossen.
2. **app.js**: Die Deduplizierung beim Zusammenführen verpasster Nachrichten verwendet
   jetzt nur den **Nachrichtentext** als Schlüssel (statt `time + '|' + text`), da der
   Text zuverlässiger ist als Zeitstempel aus unterschiedlichen Quellen.

**Geänderte Dateien**: `sw.js`, `app.js`

## Geänderte Dateien (Zusammenfassung)

| Datei | Änderung |
|-------|----------|
| `sw.js` | Push-Handler: exklusive Wahl zwischen postMessage (Tabs offen) und IndexedDB (kein Tab offen) statt beides gleichzeitig |
| `app.js` | Missed-Messages-Handler: textbasierte Deduplizierung statt zeitbasierter |

## Dokumentation aktualisiert

- `DOKUMENTATION.md` — Abschnitte 5.2 (Service Worker), 9.1 (Gesamtablauf), 11 (Offline-Nachrichten, Service Worker Verhalten) aktualisiert
- `RELEASE_NOTES.md` — Diesen Eintrag hinzugefügt

## Hinweise zum Update

1. **push-service**: Neu bauen und deployen (`ant compile && ant jar && ant dist`)
2. **SMSCenterExt**: Keine Änderungen nötig
3. **Browser**: Spieler müssen **Ctrl+F5** drücken (oder den Service Worker in den DevTools
   deregistrieren), damit die aktualisierte `sw.js` geladen wird

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
