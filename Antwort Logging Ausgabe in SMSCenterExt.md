# Antwort: Logging Ausgabe in SMSCenterExt

## Analyse des Problems

Michael beobachtet, dass trotz Konfiguration von `log4j.rootLogger='INFO'` in der
`log4j.properties` weiterhin DEBUG-Ausgaben der smslib in der NetBeans-Console erscheinen.

## Ursache 1: Syntaxfehler in log4j.properties

In der gezeigten Konfiguration steht:

```properties
log4j.rootLogger='INFO', stdout
```

Die **einfachen Anführungszeichen** um `'INFO'` sind **ungültig** für log4j 1.x.
log4j kann den Level nicht parsen und fällt auf den Default-Level **DEBUG** zurück.

**Korrekt** (ohne Anführungszeichen):

```properties
log4j.rootLogger=INFO, stdout
```

## Ursache 2: Wie smslib das Logging intern handhabt

Die smslib v3 verwendet eine eigene Wrapper-Klasse `org.smslib.helper.Logger`, die
intern einen `org.apache.log4j.Logger` mit dem Namen **"smslib"** erstellt.

Durch Dekompilierung des Bytecodes in `smslib-v3.jar` ergibt sich folgender Ablauf
im Konstruktor von `org.smslib.helper.Logger`:

```
Konstruktor Logger():
  WENN VM = IKVM.NET:
    WENN Datei "log4j.properties" im aktuellen Verzeichnis existiert:
      → Logger.getLogger("smslib") erstellen
      → PropertyConfigurator.configure("log4j.properties") aufrufen
    SONST:
      → log4jLogger = null (kein Logging)
  SONST (normale JVM, z.B. Oracle/OpenJDK):
    → Logger.getLogger("smslib") erstellen
    → KEIN PropertyConfigurator-Aufruf!
```

Das bedeutet: Auf einer normalen JVM (wie bei NetBeans) ruft smslib **nicht**
`PropertyConfigurator.configure()` auf. Stattdessen verlässt es sich auf die
automatische Konfiguration von log4j 1.x:

- log4j 1.x sucht beim ersten Logger-Aufruf automatisch nach `log4j.properties`
  **im Classpath**
- Wird die Datei nicht gefunden, oder ist sie fehlerhaft (z.B. wegen der Anführungszeichen),
  verwendet log4j die Default-Konfiguration: **DEBUG-Level mit ConsoleAppender**

### Zwei Logging-Frameworks in SMSCenterExt

SMSCenterExt verwendet **zwei verschiedene** Logging-Frameworks:

| Komponente | Framework | Konfiguration |
|-----------|-----------|---------------|
| SMSCenterExt eigener Code (GUI, etc.) | `java.util.logging` (JUL) | Über JVM-Properties oder programmatisch |
| smslib-v3 (Gateway, Router, etc.) | `org.apache.log4j` (log4j 1.x) | Über `log4j.properties` im Classpath |

Die `java.util.logging`-Konfiguration hat **keinen Einfluss** auf log4j und umgekehrt.

## Lösung

### Schritt 1: log4j.properties korrigieren und richtig platzieren

Die Datei `log4j.properties` muss im **Classpath** liegen. Bei NetBeans-Projekten
ist das der `src/`-Ordner (Dateien daraus werden beim Build nach `build/classes/`
kopiert).

Datei: **`SMSCenterExt/src/log4j.properties`**

```properties
# Root logger: INFO level, output to console
log4j.rootLogger=INFO, stdout

# Console appender
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n

# Optional: smslib-spezifisches Level (falls abweichend vom Root gewuenscht)
# log4j.logger.smslib=DEBUG
```

**Wichtig**: Kein `'INFO'` mit Anführungszeichen, sondern einfach `INFO`.

### Schritt 2: Clean and Build

Nach dem Anlegen/Ändern der Datei muss ein **Clean and Build** (Shift+F11) in NetBeans
ausgeführt werden, damit die `log4j.properties` nach `build/classes/` kopiert wird
und im Classpath liegt.

### Schritt 3: Steuerung des Log-Levels

Mit der korrekten `log4j.properties` kann das Level flexibel gesteuert werden:

| Gewünschtes Verhalten | Konfiguration |
|-----------------------|---------------|
| Alles ab INFO (kein DEBUG) | `log4j.rootLogger=INFO, stdout` |
| Alles ab INFO, aber smslib auf DEBUG | `log4j.rootLogger=INFO, stdout` + `log4j.logger.smslib=DEBUG` |
| Nur Warnungen und Fehler | `log4j.rootLogger=WARN, stdout` |
| Komplett stumm | `log4j.rootLogger=OFF` |

### Schritt 4 (optional): Logging in Datei

Für den Produktionsbetrieb kann zusätzlich ein File-Appender konfiguriert werden:

```properties
log4j.rootLogger=INFO, stdout, file

# Console
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n

# Datei (taegliches Rotating)
log4j.appender.file=org.apache.log4j.DailyRollingFileAppender
log4j.appender.file.File=logs/smscenter.log
log4j.appender.file.DatePattern='.'yyyy-MM-dd
log4j.appender.file.layout=org.apache.log4j.PatternLayout
log4j.appender.file.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n
```

## Zusammenfassung

| Problem | Ursache | Lösung |
|---------|---------|--------|
| DEBUG-Ausgaben trotz INFO-Konfiguration | Anführungszeichen `'INFO'` in log4j.properties sind ungültig | Anführungszeichen entfernen: `log4j.rootLogger=INFO, stdout` |
| log4j.properties wird nicht geladen | Datei nicht im Classpath | Datei in `SMSCenterExt/src/` ablegen + Clean and Build |
| SMSCenterExt-eigene Logs vs. smslib-Logs | Zwei verschiedene Frameworks (JUL vs. log4j) | log4j.properties steuert nur smslib; JUL-Config ist separat |
