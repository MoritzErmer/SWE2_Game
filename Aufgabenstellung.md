# Software Engineering II TES23  
**Prof. Dr. Katharina Nerz**  
**DHBW Stuttgart 2026**

# Programmentwurf

## Aufgabe

Entwickeln Sie ein **Echtzeit-Spiel**, welches auf der **Konsole** oder einer von Ihnen geschriebenen **GUI** läuft.

(Einbindung von externen Libraries ist erlaubt, darf aber nicht der Kern Ihrer Arbeit sein).

Als Programmiersprache dürfen Sie zwischen **Java** und **C++** wählen.

Zentrale Eigenschaft Ihres Spiels muss sein, dass es sich um eine **Multithreading-Anwendung** handelt.  
Dies bedeutet nicht nur, dass die Anwendung mehrere Threads besitzt, sondern auch, dass die Nutzung von Nebenläufigkeit **essenziell** ist, das Spiel also **ohne Multithreading nicht programmierbar** wäre.

Darüber hinaus sind Sie thematisch frei.

---

# Rahmenbedingungen

- Der Programmentwurf wird in **Teams aus zwei oder drei Personen** bearbeitet, die gemeinsam abgeben.
- Die Nutzung eines unterstützenden **LLMs wie GitHub Copilot** ist erlaubt.
- Sie erstellen den Programmentwurf teils in der Vorlesungszeit, teils in den **102 Stunden Selbststudium**.  
  Der Umfang Ihres Projekts sollte so gewählt sein, dass die Aufgabe in dieser Zeit lösbar ist.
- Sie dürfen sich von der Aufgabenstellung des Programmentwurfs **Software Engineering II des Jahrgangs TES21** inspirieren lassen.  
  Der Umfang des damaligen Programmentwurfs wäre aber **zu klein für Ihre Abgabe**.

Zum Vergleich:  
Die Studierenden aus TES21 hatten für ihre damalige Aufgabe mit vorgegebenen Spielregeln und Requirements **zwei Wochen Zeit**.  
Sie müssen zwar **Spielregeln und Requirements selbst erarbeiten**, haben aber durch die **semesterbegleitende Bearbeitung deutlich mehr Zeit**.

- In der **letzten Vorlesungseinheit** stellen Sie als Team in einer **10-minütigen Präsentation** Ihr Projekt vor, inkl. **Live-Demo**.
- **Abgabe ist eine Woche nach der Projektpräsentation am xx.4.2026.**

---

# Bestandteile Ihrer Abgabe

Ihre Abgabe muss die folgenden Bestandteile enthalten:

## 1. Ein PDF-Dokument (oder mehrere)

Dieses enthält:

- Eine **kurze Beschreibung der Projektidee**
- Eine **Erläuterung der Regeln Ihres implementierten Spiels**
- Eine **Erläuterung des Multithreading-Charakters Ihrer Software**, d.h.:

  - Auflistung der **Threads im Programm**
  - kurze Beschreibung ihrer **Funktionalität**
  - Beschreibung der **in mehreren Threads genutzten Ressourcen**
  - Erklärung, **wie der Zugriff kontrolliert wird**, um inkonsistentes Verhalten auszuschließen

- Die von Ihnen **erarbeiteten Requirements**, die Ihre Software erfüllt
- Falls zutreffend: **Erläuterung der verwendeten Designpatterns und Architekturmuster**
- **Antworten auf die unten genannten Fragen zum Thema Verlässlichkeit**

## 2. Den Quellcode Ihrer Software

- geschrieben in **Java oder C++**
- **kompiliert fehlerfrei**

---

# Fragen an Ihr entwickeltes Spiel zum Thema Verlässlichkeit

Beantworten Sie in Ihrer Abgabe die folgenden Fragen zum Thema **Verlässlichkeit**.  
Die Seitenangaben beziehen sich auf den **Foliensatz zum Thema Verlässliche Systeme**.

### 1. Folgen eines Systemausfalls

Welche Folgen (vgl. Foliensatz **Seite 7**) könnte ein **Systemausfall Ihres Spiels** haben?  
Wie **kritisch** bewerten Sie dies?

### 2. Mechanismen zur Verlässlichkeit

Welche **Mechanismen** haben Sie verwendet, um die **Verlässlichkeit Ihres Spiels zu erhöhen**?

### 3. Metriken für Zuverlässigkeit und Verfügbarkeit

Welche **Metrik(en) für Zuverlässigkeit und Verfügbarkeit** (vgl. Foliensatz **Seite 44 ff**) sind Ihrer Meinung nach **für Ihr Spiel am besten geeignet**?

Welche **Wahrscheinlichkeits-Werte** würden Sie verlangen?

### 4. Testdatensatz

Beschreiben Sie **qualitativ**, wie Sie einen **Testdatensatz** (vgl. Foliensatz **Seite 65 ff**) erstellen würden, um nachzuweisen, dass Ihre **Metrik den gewählten Wahrscheinlichkeitswert einhält**.

Für Frage **3 und 4** sollten Sie **Vergleichswerte recherchieren**.

- Nennen Sie Ihre **Quellen**
- Begründen Sie, weshalb die **Vergleichswerte auf Ihr Spiel übertragbar sind**
- oder weshalb Sie **eigene Werte anders gewählt haben**

---

# Bewertungskriterien

| Punkte | Kriterium |
|------|------|
| 5 | Im PDF: Originalität der Projektidee, passender Umfang des Projekts |
| 10 | Im PDF: Erklärung der Spielregeln, Erläuterung von Architekturmustern oder Designpatterns, Beschreibung wichtiger Aspekte Ihrer Software (außer Multithreading-Eigenschaften) |
| 10 | Im PDF: Die Multithreading-Eigenschaften des Spiels werden beschrieben (Threads, gemeinsam genutzte Ressourcen, Synchronisation gegen inkonsistentes Verhalten) |
| 10 | Im PDF: Die Requirements sind konsistent, vollständig, realisierbar und verifizierbar und bilden geeignet die Spielregeln sowie funktionale und nicht-funktionale Anforderungen ab |
| 10 | Im PDF: Die Fragen zum Thema Verlässlichkeit werden beantwortet |
| 10 | Im Quellcode: Das Programm ist in Java oder C++ geschrieben und kompiliert fehlerfrei. Das Spiel ist über Konsole oder GUI spielbar |
| 15 | Im Quellcode: Das Programm enthält **mindestens drei Threads**. Gemeinsam genutzte Ressourcen sind geeignet vor Inkonsistenzen durch asynchronen Zugriff geschützt |
| 10 | Im Quellcode: Projekt ist gut strukturiert, leicht zu lesen, verwendet verständliche Benennungen und ist ausreichend kommentiert. Das Projekt folgt **Clean Code** Richtlinien |

---

# Bonus

**Maximal 10 Bonuspunkte**

Für:

- sehr gute **Präsentation**
- **überdurchschnittlichen Spielspaß**
- **sehr großen Projektumfang**
- **besonders gute PDF-Dokumentation**

Pro Kategorie sind **2–3 Bonuspunkte** möglich.

---

**Viel Erfolg!**