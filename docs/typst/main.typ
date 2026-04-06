#set page(paper: "a4", margin: (x: 2.2cm, y: 2.2cm))
#set text(lang: "de", size: 11pt)
#set par(justify: true, leading: 0.68em)
#set heading(numbering: "1.1.1", depth: 3)
#set math.equation(numbering: "(1)")

#let repo = "SWE2_Game"
#let course = "Software Engineering II (TES23)"
#let semester = "WS 2025/2026"

#set document(
  title: [Programmentwurf: Multithreaded 2D Automation Game],
)

#title()

*Kurs:* #course  \
*Semester:* #semester  \
*Datum:* 8.04.2026 \
*Gruppenmitglieder:* Moritz Ermer, Tom Gelhorn, Malte Schröter

= Projektidee

Das Projekt implementiert ein zweidimensionales Echtzeit-Automationsspiel mit grafischer Benutzeroberfläche (Java Swing). Die Spielenden bewegen sich auf einer tilebasierten Karte, bauen Ressourcen ab und errichten eine Produktionskette aus Minern, Greifern, Förderbändern und Schmelzöfen. Das Spiel ist als nebenläufiges System entworfen: Produktion, Logistik, Kollisionserkennung und Benutzerinteraktion laufen parallel.

Die Spielidee ist bewusst auf die Verbindung von Gameplay und Nebenläufigkeit ausgelegt. Ein sequentielles Modell wäre für die geforderte Echtzeit-Simulation mit simultanen Entitäten nicht geeignet, da Fortschritt und Reaktionszeit von Produktion und Transport ansonsten direkt an die UI-Ereignisse gekoppelt wären.

= Spielregeln und Gameplay

== Kernspielschleife

1. Die Spielfigur bewegt sich per Tastatur (WASD) über die Karte.
2. Auf Ressourcentiles können Vorkommen per Enter-Taste abgebaut werden.
3. Aus gewonnenen Materialien werden über das Crafting-Menü Maschinen und Infrastruktur hergestellt.
4. Maschinen werden auf der Karte platziert und arbeiten autonom.
5. Ziel ist der Aufbau einer stabilen Produktionspipeline mit kontinuierlichem Materialfluss.

== Interaktion und Bedienung

- `WASD`: Bewegung
- `Enter`: manuelles Mining
- `E`: Inventar
- `C`: Crafting-Menü
- `1..9`: Hotbar-Auswahl
- Linksklick: Infrastruktur platzieren
- Rechtsklick: Maschineninteraktion (Input/Output)
- `R`: Rotation platzierter Maschinen
- `Q`: Dekonstruktion (Maschinen und Förderbänder)

== Wirtschaft und Produktion

- Miner erzeugen Erz aus Lagerstaetten.
- Greifer transportieren Items zwischen Quell- und Zieltile.
- Schmelzer wandeln Erz in Platten um.
- Crafting erzeugt neue Infrastrukturkomponenten aus Zwischenprodukten.

= Multithreading-Charakter

== Thread-Landschaft

Die Anwendung nutzt die folgenden Thread-Kategorien:

1. `EDT` (Swing Event Dispatch Thread): Rendering und UI-Ereignisse.
2. `ScheduledExecutorService` (Maschinen-Pool): periodische `tick()`-Ausführung je Maschine.
3. `Logistics-Thread` (SingleThreadExecutor): globale Förderbandlogik.

Damit ist die Forderung nach mindestens drei Threads erfüllt.

== Gemeinsam genutzte Ressourcen

- Weltzustand (`WorldMap`, `Tile`-Objekte)
- Maschinenlisten, Beltlisten
- Maschineninterne Puffer (`inputBuffer`, `outputBuffer`)
- Inventarzustand der Spielfigur

== Synchronisationsmechanismen

1. Pro Tile existiert ein `ReentrantLock` fuer feingranulare Synchronisation.
2. Mehrtile-Operationen nutzen konsistente Lock-Reihenfolgen (Deadlock-Praevention).
3. Supervisor-Container fuer Maschinen/Belts/Roboter nutzen thread-sichere Listen (`CopyOnWriteArrayList`) und `ConcurrentHashMap` fuer Registrierungsindizes.
4. Lebenszyklus-Flags verwenden `AtomicBoolean` fuer nebenlaeufige Sichtbarkeit.
5. Positionswerte von Robotern und Spielerzustand sind fuer Sichtbarkeit auf volatile/atomaren Datentypen aufgesetzt.

= Anforderungen (Requirements)

Die folgenden Anforderungen sind konsistent, verifizierbar und umsetzbar formuliert.

#table(
  columns: (7%, 53%, 20%, 20%),
  inset: 6pt,
  stroke: 0.5pt,
  [*ID*], [*Anforderung*], [*Typ*], [*Verifikation*],

  [F-01],
  [Die Anwendung muss eine spielbare 2D-Karte mit Ressourcentiles bereitstellen.],
  [Funktional],
  [Manueller Test],

  [F-02],
  [Die Spielfigur muss per Tastaturbewegung innerhalb der Weltgrenzen steuerbar sein.],
  [Funktional],
  [Unit + manuell],

  [F-03], [Ressourcenabbau auf Deposit-Tiles muss zeitabhaengig erfolgen.], [Funktional], [Integrationstest],
  [F-04],
  [Crafting muss definierte Rezepte pruefen und Ergebnisse ins Inventar uebertragen.],
  [Funktional],
  [Unit-Test],

  [F-05],
  [Maschinen (Miner, Schmelzer, Greifer) muessen platzierbar und dekonstruierbar sein.],
  [Funktional],
  [Manueller Test],

  [F-06],
  [Foerderbaender muessen als Logistikobjekte registriert werden und Items transportieren.],
  [Funktional],
  [Integrationstest],

  [F-07],
  [Das System muss mindestens drei parallele Threads mit Spielverantwortung nutzen.],
  [Funktional],
  [Architektur-Nachweis],

  [F-08],
  [Gemeinsam genutzte Ressourcen muessen gegen Inkonsistenz durch Synchronisation geschuetzt sein.],
  [Nicht-funktional],
  [Code-Review + Tests],

  [F-09],
  [Das Projekt muss fehlerfrei kompilierbar und als standalone JAR startbar sein.],
  [Nicht-funktional],
  [Build-Pipeline],

  [F-10], [Unter Windows soll ein EXE-Paket ueber `jpackage` erzeugbar sein.], [Nicht-funktional], [Packaging-Skript],
)

= Architektur und Design Patterns

== Schichten und Module

- `world`: Spielfeldmodell und Tile-Synchronisation
- `entity`: Spieler- und Itemmodell
- `machine`: Produktionslogik und Strategien
- `logistics`: Foerderbaender, Roboter, Logistikthread
- `core`: Supervisor und Kollisionskontrolle
- `ui`: Rendering und Eingabeverarbeitung

== Verwendete Muster

1. *Strategy Pattern*: Produktionsvarianten (`MiningStrategy`, `SmeltingStrategy`, `GrabberStrategy`) entkoppeln Maschinenklasse und Prozesslogik.
2. *Supervisor Pattern*: `GameSupervisor` steuert Start/Stopp und Lebenszyklus aller Hintergrundaktivitaeten.
3. *Producer-Consumer-Mechanik*: Maschinen erzeugen Output, Logistikkomponenten transportieren und konsumieren diesen asynchron.

= Verlaesslichkeit

== Folgen eines Systemausfalls

Ein Ausfall hat primaer Folgen auf Spielverlauf und Nutzungsqualitaet:

1. Verlust des aktuellen Spielzustands (kein persistenter Save-Mechanismus).
2. Abbruch der laufenden Session und potentielle Frustration.
3. Im Fehlerfall moegliche Inkonsistenzen (z.B. doppelte oder verlorene Items) bei unvollstaendigen Mehrschritttransaktionen.

Kritikalitaet: *niedrig bis mittel*. Es handelt sich nicht um ein sicherheitskritisches System, jedoch ist die Verfuegbarkeit fuer Spielspass relevant.

== Mechanismen zur Erhoehung der Verlaesslichkeit

1. Granulare Locks pro Tile zur Begrenzung kritischer Bereiche.
2. Definierte Lock-Reihenfolge bei Mehrtilezugriffen zur Deadlock-Vermeidung.
3. Gekapselter Thread-Lifecycle mit zentralem Start/Stopp im Supervisor.
4. Explizite Fehlerabfangung in periodischen Maschinentasks, damit Einzeldefekte nicht den gesamten Scheduler abbrechen.
5. Testpyramide (Unit/Integration/End-to-End-Smoketests) fuer regressionsarme Weiterentwicklung.

== Metriken fuer Zuverlaessigkeit und Verfuegbarkeit

Geeignete Metriken:

1. *Session Success Rate (SSR)*: Anteil erfolgreich abgeschlossener Spielsitzungen ohne Crash.
2. *Mean Time Between Failures (MTBF)* fuer Sessions.
3. *Availability* auf Anwendungsebene waehrend aktiver Spielzeit.

Vorgeschlagene Zielwerte (projektangemessen):

- SSR >= 99.0% ueber definierte Testsessionlaenge.
- Verfuegbarkeit >= 99.5% innerhalb eines Testfensters.
- MTBF >= 50 Stunden aggregierter Laufzeit im Lasttest.

Diese Werte orientieren sich an gaengigen Verfuegbarkeitsklassen aus dem Cloud-Umfeld und werden auf ein nicht-sicherheitskritisches Unterhaltungsprodukt skaliert @aws-sla @azure-sla.

Aktueller Nachweisstand im Projekt:

1. Eine dedizierte Reliability-Testklasse liefert baseline-faehige Ausgaben fuer SSR und MTBF.
2. Die Ergebnisse werden bei der Testausfuehrung in der Konsole ausgegeben und fuer die Abgabe dokumentiert.
3. Die Zielwerte werden bis zur Endabgabe ueber wiederholte Testlaeufe auf dem Zielsystem validiert.

== Qualitativer Testdatensatz

Zur Nachweisfuehrung wird ein strukturierter Datensatz aus automatisierten und manuellen Runs vorgeschlagen:

1. *Szenarioklassen*: Leerlauf, Normalbetrieb, hohe Maschinendichte, hohe Logistiklast, wiederholter Start/Stopp.
2. *Lastvariation*: unterschiedliche Karten- und Objektgroessen, steigende Tick-Raten.
3. *Fehlerinduktion*: gezielte Unterbrechung einzelner Threads bzw. delayed operations.
4. *Messpunkte*: Session-Start/Ende, Exceptions, Thread-Liveness, Item-Konsistenzchecks.
5. *Auswertung*: SSR, MTBF und Verfuegbarkeit je Szenarioklasse sowie Gesamtmittel.

Die Uebertragbarkeit externer Vergleichswerte wird darueber begruendet, dass auch hier eine interaktive Anwendung mit Benutzerfokus betrachtet wird, bei der wahrgenommene Ausfaelle primaer ueber Sessionabbrueche sichtbar werden.

= Testkonzept und Nachweisstruktur

== Unit-Tests

- `WorldMapTest`: Transferlogik inkl. Belegungsfaelle.
- `CraftingManagerTest`: Rezeptvalidierung und Materialkonsum.

== Integrationstests

- `ProductionPipelineIntegrationTest`: Miner-Greifer-Schmelzer-Pipeline bis zum nachweisbaren Output.
- `MachineStressAndReliabilityTest` (`@Tag("load")`): Parallelisierte Lastszenarien mit mehreren Sessions.
- `MachineStressAndReliabilityTest` (`@Tag("stress")`): Schnelle Register-/Deregister-Zyklen fuer Maschinen.
- `MachineStressAndReliabilityTest` (`@Tag("reliability")`): Baseline-Metriken fuer SSR/MTBF.

== End-to-End-Smoketest

- `SupervisorLifecycleE2ETest`: Start/Stop des Gesamtsystems ohne Deadlock.

= Build, Standalone und EXE-Bereitstellung

== Standalone JAR

Die Anwendung ist als ausfuehrbares Maven-Artefakt konfiguriert. Start ueber:

```bash
mvn clean package
java -jar target/swe2-game-<version-from-pom>.jar
```

== Windows-EXE

Mit JDK 17+ (inkl. `jpackage`) kann eine EXE erzeugt werden:

```powershell
./scripts/package-exe.ps1
```

Das Skript erzeugt ein installierbares Paket im Verzeichnis `dist`.

Zusatz fuer reproduzierbare Freigaben:

1. `./scripts/check-version-artifacts.ps1`
2. `./scripts/release-gate.ps1`

Die finale Freigabe orientiert sich an der Checkliste in `DEPLOYMENT_CHECKLIST.md`.

= Fazit

Der aktuelle Projektstand erfuellt die Kernelemente der Aufgabenstellung: spielbare GUI, essentielle Nebenläufigkeit mit mehreren Threads, synchronisierte gemeinsame Ressourcen, strukturierte Anforderungen sowie ein belastbares Test- und Packaging-Fundament. Fuer die finale Abgabe empfiehlt sich die Ergaenzung empirischer Lasttestergebnisse fuer die in Kapitel 6 definierten Metriken.

#bibliography("references.bib")
