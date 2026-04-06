# Requirements Traceability Matrix

Diese Matrix verknuepft die Anforderungen und Bewertungspunkte aus [Aufgabenstellung.md](Aufgabenstellung.md) mit konkreten Implementierungs- und Nachweisartefakten.

## A. Bewertungsrubrik -> Nachweise

| Quelle Aufgabenstellung | Kriterium | Implementierung | Test/Nachweis | Dokumentation |
|---|---|---|---|---|
| Punkte 5 | Originalitaet und Umfang | [src/game/Main.java](src/game/Main.java), [src/game/ui/GameUI.java](src/game/ui/GameUI.java), [src/game/machine/Grabber.java](src/game/machine/Grabber.java) | Manuelle Demo | [docs/typst/main.typ](docs/typst/main.typ#L21) |
| Punkte 10 | Spielregeln, Muster, Softwareaspekte | [src/game/ui/GameUI.java](src/game/ui/GameUI.java), [src/game/crafting/CraftingManager.java](src/game/crafting/CraftingManager.java), [src/game/machine/ProductionStrategy.java](src/game/machine/ProductionStrategy.java) | Manual Gameplay Checklist | [docs/typst/main.typ](docs/typst/main.typ#L27), [docs/typst/main.typ](docs/typst/main.typ#L100) |
| Punkte 10 | Multithreading-Eigenschaften | [src/game/core/GameSupervisor.java](src/game/core/GameSupervisor.java), [src/game/logistics/LogisticsThread.java](src/game/logistics/LogisticsThread.java) | [src/test/java/game/integration/SupervisorLifecycleE2ETest.java](src/test/java/game/integration/SupervisorLifecycleE2ETest.java) | [docs/typst/main.typ](docs/typst/main.typ#L52) |
| Punkte 10 | Konsistente, verifizierbare Requirements | [pom.xml](pom.xml), [src/game](src/game) | Unit + Integrationstests | [docs/typst/main.typ](docs/typst/main.typ#L81), [docs/typst/main.typ](docs/typst/main.typ#L82) |
| Punkte 10 | Verlaesslichkeit beantwortet | Laufzeitarchitektur in [src/game/core/GameSupervisor.java](src/game/core/GameSupervisor.java) und Tile-Locking in [src/game/world/WorldMap.java](src/game/world/WorldMap.java) | Geplante Reliability-Messung | [docs/typst/main.typ](docs/typst/main.typ#L114) |
| Punkte 10 | Quellcode Java/C++ und spielbar | [src/game/Main.java](src/game/Main.java), [src/game/ui/GameUI.java](src/game/ui/GameUI.java) | Build und Start | [docs/typst/main.typ](docs/typst/main.typ#L177) |
| Punkte 15 | Mindestens drei Threads + Schutz gemeinsamer Ressourcen | [src/game/core/GameSupervisor.java](src/game/core/GameSupervisor.java), [src/game/world/Tile.java](src/game/world/Tile.java), [src/game/world/WorldMap.java](src/game/world/WorldMap.java) | [src/test/java/game/integration/ProductionPipelineIntegrationTest.java](src/test/java/game/integration/ProductionPipelineIntegrationTest.java) | [docs/typst/main.typ](docs/typst/main.typ#L71) |
| Punkte 10 | Struktur/Clean Code/Benennungen/Kommentare | Paketstruktur unter [src/game](src/game) | Code Review | [docs/typst/main.typ](docs/typst/main.typ#L100) |

## B. Funktionale Requirement-IDs (F-01 bis F-10)

| ID | Implementierung | Test/Nachweis | Dokumentation |
|---|---|---|---|
| F-01 | [src/game/world/WorldMap.java](src/game/world/WorldMap.java), [src/game/ui/GameUI.java](src/game/ui/GameUI.java) | Manuelle Kartenbegehung | [docs/typst/main.typ](docs/typst/main.typ#L81) |
| F-02 | [src/game/entity/PlayerCharacter.java](src/game/entity/PlayerCharacter.java) | Manuelle Eingabetests | [docs/typst/main.typ](docs/typst/main.typ#L81) |
| F-03 | [src/game/ui/GameUI.java](src/game/ui/GameUI.java), [src/game/world/TileType.java](src/game/world/TileType.java) | [src/test/java/game/integration/ProductionPipelineIntegrationTest.java](src/test/java/game/integration/ProductionPipelineIntegrationTest.java) | [docs/typst/main.typ](docs/typst/main.typ#L81) |
| F-04 | [src/game/crafting/CraftingManager.java](src/game/crafting/CraftingManager.java) | [src/test/java/game/crafting/CraftingManagerTest.java](src/test/java/game/crafting/CraftingManagerTest.java) | [docs/typst/main.typ](docs/typst/main.typ#L81) |
| F-05 | [src/game/ui/GameUI.java](src/game/ui/GameUI.java), [src/game/machine](src/game/machine) | Manuelle Platzierungs-/Abbau-Tests | [docs/typst/main.typ](docs/typst/main.typ#L81) |
| F-06 | [src/game/core/GameSupervisor.java](src/game/core/GameSupervisor.java), [src/game/logistics/LogisticsThread.java](src/game/logistics/LogisticsThread.java) | [src/test/java/game/world/WorldMapTest.java](src/test/java/game/world/WorldMapTest.java) | [docs/typst/main.typ](docs/typst/main.typ#L81) |
| F-07 | [src/game/core/GameSupervisor.java](src/game/core/GameSupervisor.java) | [src/test/java/game/integration/SupervisorLifecycleE2ETest.java](src/test/java/game/integration/SupervisorLifecycleE2ETest.java) | [docs/typst/main.typ](docs/typst/main.typ#L81) |
| F-08 | [src/game/world/Tile.java](src/game/world/Tile.java), [src/game/world/WorldMap.java](src/game/world/WorldMap.java) | [src/test/java/game/world/WorldMapTest.java](src/test/java/game/world/WorldMapTest.java) | [docs/typst/main.typ](docs/typst/main.typ#L81) |
| F-09 | [pom.xml](pom.xml), [src/game/Main.java](src/game/Main.java) | Buildlauf und Starttest | [docs/typst/main.typ](docs/typst/main.typ#L81) |
| F-10 | [scripts/package-exe.ps1](scripts/package-exe.ps1), [scripts/verify-jdk17.ps1](scripts/verify-jdk17.ps1) | Packaging-Lauf | [docs/typst/main.typ](docs/typst/main.typ#L81) |

## C. Verlaesslichkeitsfragen -> Nachweise

| Frage in Aufgabenstellung | Antwortstelle in Doku | Technische Basis |
|---|---|---|
| 1. Folgen eines Systemausfalls | [docs/typst/main.typ](docs/typst/main.typ#L116) | [src/game/core/GameSupervisor.java](src/game/core/GameSupervisor.java), [src/game/ui/GameUI.java](src/game/ui/GameUI.java) |
| 2. Mechanismen zur Verlaesslichkeit | [docs/typst/main.typ](docs/typst/main.typ#L124) | [src/game/world/WorldMap.java](src/game/world/WorldMap.java), [src/game/world/Tile.java](src/game/world/Tile.java), [src/game/core/GameSupervisor.java](src/game/core/GameSupervisor.java) |
| 3. Metriken fuer Zuverlaessigkeit/Verfuegbarkeit | [docs/typst/main.typ](docs/typst/main.typ#L132) | Teststrategie in [docs/typst/main.typ](docs/typst/main.typ#L165) |
| 4. Qualitativer Testdatensatz | [docs/typst/main.typ](docs/typst/main.typ#L145) | Testgrundlage in [src/test/java/game](src/test/java/game) |

## D. Pflichtprozess fuer Doku-Konsistenz

Bei jeder funktionalen Aenderung sind diese Updates verpflichtend:

1. Codeanpassung in [src/game](src/game).
2. Testanpassung in [src/test/java/game](src/test/java/game).
3. Dokuanpassung in [docs/typst/main.typ](docs/typst/main.typ).
4. Traceability-Update in [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md).

## E. Definition of Done (DoD)

Eine Aenderung gilt nur dann als abgeschlossen, wenn:

1. Build/Test erfolgreich sind.
2. Doku die geaenderte Funktion und den Requirementbezug enthaelt.
3. Die Matrix den Nachweislink auf Code plus Test plus Doku enthaelt.
4. Die Version-/Artefakt-Pruefung ohne Fehler durchlaeuft.
