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
