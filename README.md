# FactoryGame

Ein 2D-Automatisierungsspiel, entwickelt im Rahmen von SWE2 an der DHBW Stuttgart.

## Download

[![Download latest release](https://img.shields.io/github/v/release/MoritzErmer/SWE2_Game?label=Download&style=for-the-badge)](https://github.com/MoritzErmer/SWE2_Game/releases/latest)

> **Keine Java-Installation nötig** — die EXE enthält eine eingebettete JRE.

Auf der [Releases-Seite](https://github.com/MoritzErmer/SWE2_Game/releases/latest) die Datei `FactoryGame_x.x.x.exe` herunterladen und ausführen.

## Spielmodi

| Modus | Beschreibung |
|---|---|
| **Normal** | Ressourcen abbauen, verarbeiten und craften |
| **Kreativ** | Alle Rezepte ohne Materialkosten craftbar |

## Spielziel

- Auf jeder Karte spawnt eine 4x4 Rakete an einer zufaelligen Position.
- Ziel ist die Reparatur der Rakete mit genau folgenden Teilen:
	- 80 Iron Gears
	- 100 Copper Plates
	- 10 Conveyor Belts
- Die Rakete kann nur manuell per Rechtsklick mit dem aktuell ausgewaehlten Hotbar-Item gefuettert werden.
- Auf Raketen-Tiles koennen keine Maschinen, Foerderbaender oder Boden-Items platziert werden.
- Nach vollstaendiger Reparatur startet die Rakete automatisch und es erscheint ein Endscreen mit der benoetigten Gesamtspielzeit.

## Spielstand

- **Ctrl+S** — Spielstand speichern (`%APPDATA%/SWE2-Game/save.json`)
- **Ctrl+L** — Spielstand laden (alternativ über Hauptmenü → Laden)

## Entwicklung

```bash
# Bauen
mvn clean package

# Spiel starten
mvn exec:java@run

# Tests ausführen
mvn clean test

# Windows-EXE lokal erzeugen (benötigt JDK 17+ und WiX 3.x)
./scripts/package-exe.ps1
```

## CI/CD

Jeder Push auf `main` baut automatisch eine neue `FactoryGame_1.0.{run_number}.exe` und veröffentlicht sie als GitHub Release.
