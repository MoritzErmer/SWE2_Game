---
name: Standalone Packaging Architecture
description: jlink + jpackage approach chosen for standalone Windows EXE; exec-maven-plugin added to pom.xml
type: project
---

Standalone-Distribution via `jdeps` → `jlink` (minimale JRE) → `jpackage --type exe`.

**Why:** Endnutzer sollen keine Java-Installation benötigen. jlink reduziert die gebündelte JRE von ~150 MB auf ~30–40 MB durch Beschränkung auf tatsächlich benötigte Module (ermittelt automatisch per jdeps).

**How to apply:** Build-Skript ist `scripts/package-exe.ps1`. Erfordert JDK 17+, Maven und WiX Toolset 3.x auf dem Build-Rechner. Ausgabe landet in `dist/SWE2-Game-<version>.exe`.

`exec-maven-plugin 3.3.0` mit Execution-ID `run` wurde in `pom.xml` ergänzt, damit `mvn exec:java@run` funktioniert.
