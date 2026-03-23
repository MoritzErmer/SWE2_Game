# Deployment Checklist (Standalone Windows)

Diese Checkliste ist das finale Release-Gate vor Abgabe/Distribution.

## 1. Preconditions

- [ ] JDK 17+ installiert und in PATH.
- [ ] Maven installiert und in PATH.
- [ ] `jpackage` verfuegbar.
- [ ] Projekt auf aktuellem Commit-Stand.

## 2. Konsistenzchecks

- [ ] `./scripts/check-doc-consistency.ps1` erfolgreich.
- [ ] `./scripts/check-version-artifacts.ps1` erfolgreich.
- [ ] Traceability in [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md) ist aktuell.

## 3. Test- und Reliability-Gate

- [ ] `mvn clean test` erfolgreich.
- [ ] Load-Test (`@Tag("load")`) erfolgreich.
- [ ] Stress-Test (`@Tag("stress")`) erfolgreich.
- [ ] Reliability-Baseline (`@Tag("reliability")`) gibt SSR/MTBF ohne Fehler aus.

## 4. Packaging-Gate

- [ ] `./scripts/package-exe.ps1` erfolgreich.
- [ ] Exe-Artefakt vorhanden in `dist/`.
- [ ] Exe startet lokal ohne IDE.

## 5. Manuelle Gameplay-Pruefung

- [ ] Start und Rendering funktionieren.
- [ ] Bewegung (WASD) reagiert korrekt.
- [ ] Mining, Crafting und Maschinenplatzierung funktionieren.
- [ ] Foerderbaender und Maschinenlogik laufen stabil.
- [ ] Sauberes Beenden ohne haengende Prozesse.

## 6. Dokumentations-Gate

- [ ] [docs/typst/main.typ](docs/typst/main.typ) deckt alle Punkte aus [Aufgabenstellung.md](Aufgabenstellung.md) ab.
- [ ] Verlaesslichkeitsfragen 1-4 sind beantwortet.
- [ ] Build-/Packaging-Anleitung entspricht den aktuellen Skripten.

## 7. One-shot Gate Command

```powershell
./scripts/release-gate.ps1
```

Erwartung: Alle Checks erfolgreich und EXE-Artefakt erstellt.
