# Thread-Robustheit & Failsafe-Konzept

## Überblick

Das Spiel nutzt zwei parallele Ausführungsebenen:

| Ebene | Implementierung | Zweck |
|-------|----------------|-------|
| **Maschinen-Pool** | `ScheduledExecutorService` (N Threads = CPU-Kerne) | Jede Maschine ticked alle 500 ms |
| **Logistics-Thread** | `SingleThreadExecutor` | Fließbänder bewegen Items alle 200 ms |

---

## Fehlerbehandlung pro Schicht

### 1. Maschinen-Tasks (Producer-Schicht)

Jeder Maschinen-Task ist **individuell gesichert**:

```java
machineExecutor.scheduleAtFixedRate(() -> {
    try {
        machine.tick();
    } catch (Exception e) {
        System.err.printf("[MachineExecutor] Fehler in '%s': %s%n", machine.getName(), e);
    }
}, 0, 500, TimeUnit.MILLISECONDS);
```

**Verhalten bei Exception:**
- Der Fehler wird geloggt (inkl. `Exception.toString()`, damit auch bei `null`-Message kein NPE entsteht).
- Der Task wird beim nächsten Intervall **automatisch erneut ausgeführt** – `ScheduledExecutorService` rescheduled ihn.
- Andere Maschinen sind **vollständig isoliert** und laufen ungestört weiter.

**Zusätzliche Absicherung:** Alle Threads des Pools erhalten einen `UncaughtExceptionHandler` als letztes Sicherheitsnetz (fängt `Error`-Subklassen ab, die nicht als `Exception` behandelt werden).

---

### 2. Logistics-Thread (Belt-Schicht)

Der `LogisticsThread` war ursprünglich ungeschützt. Er ist jetzt **zweistufig abgesichert**:

#### Stufe 1 – Per-Belt-Isolation

```java
for (ConveyorBelt belt : belts) {
    try {
        processBelt(belt);
    } catch (Exception e) {
        anyError = true;
        System.err.printf("[LogisticsThread] Belt-Fehler bei (%d,%d): %s%n",
                belt.getX(), belt.getY(), e);
    }
}
```

Ein einzelner kaputtes Belt tötet **nicht** den gesamten Thread – alle anderen Belts werden weiterhin verarbeitet.

#### Stufe 2 – Consecutive-Error-Threshold → sauberer Exit

```
MAX_CONSECUTIVE_ERRORS = 5
```

Wenn in **5 aufeinanderfolgenden Ticks** (1 Sekunde) mindestens ein Belt-Fehler aufgetreten ist, beendet sich der Thread **kontrolliert** mit `return`. Ein harter Absturz wird vermieden; der Thread signalisiert dem Watchdog, dass er neu gestartet werden soll.

---

### 3. Watchdog (Supervisor-Schicht)

Der `GameSupervisor` speichert das `Future<?>` des Logistics-Threads und überwacht es **sekündlich**:

```java
watchdogFuture = machineExecutor.scheduleAtFixedRate(() -> {
    if (!running.get()) return;
    if (logisticsFuture != null && logisticsFuture.isDone()) {
        // Thread ist beendet (normal oder durch Fehler) → Neustart
        logisticsFuture = logisticsExecutor.submit(
            new LogisticsThread(map, belts, running));
    }
}, 1, 1, TimeUnit.SECONDS);
```

**Ablauf bei Thread-Absturz:**

```
T=0s    Logistics-Thread startet
T=0-1s  Belts fehlerhafte Ticks → consecutiveErrorTicks steigt
T=1s    consecutiveErrorTicks == 5 → Thread beendet sich sauber
T=1-2s  Watchdog-Intervall prüft: logisticsFuture.isDone() == true
T=2s    Watchdog startet neuen LogisticsThread
```

**Schutz gegen Race-Condition beim Shutdown:**
- `running.set(false)` wird **vor** `watchdogFuture.cancel(false)` gesetzt.
- Der Watchdog prüft `if (!running.get()) return` als ersten Guard – kein Neustart während `stop()`.
- `RejectedExecutionException` beim Submit wird gefangen (Executor könnte bereits beendet sein).

---

## Lock-Ordering (Deadlock-Prävention)

Der `Grabber` greift auf drei Tiles gleichzeitig zu (Quelle, eigenes Tile, Ziel). Deadlocks werden durch konsistente Lock-Reihenfolge nach `System.identityHashCode()` verhindert:

```java
Tile[] sorted = sortByHash(tile, srcTile, dstTile);
sorted[0].getLock().lock();
  sorted[1].getLock().lock();
    sorted[2].getLock().lock();
    // ... Transfer
    sorted[2].getLock().unlock();
  sorted[1].getLock().unlock();
sorted[0].getLock().unlock();
```

Alle `finally`-Blöcke garantieren die Freigabe auch bei Exceptions.

---

## Shutdown-Sequenz

```
stop() aufgerufen
  │
  ├─ running.set(false)          ← verhindert Watchdog-Neustart
  ├─ watchdogFuture.cancel()     ← Watchdog-Task abbrechen
  ├─ machineExecutor.shutdownNow()
  ├─ logisticsExecutor.shutdownNow()
  └─ awaitTermination(2s) für beide Pools
```

---

## Robustheitstests (`ThreadRobustnessTest.java`)

| Test | Was wird geprüft |
|------|-----------------|
| `crashingMachineDoesNotKillOtherMachines` | Eine Maschine die immer wirft, stoppt keine anderen Maschinen |
| `logisticsThreadContinuesAfterBeltException` | Guter Belt wird weiter verarbeitet trotz NPE-Belt in derselben Liste |
| `watchdogRestartsDeadLogisticsThread` | Nach Thread-Tod durch persistente Fehler: Watchdog startet ihn neu, Belts laufen wieder |
| `stopIsCleanWithPermanentlyCrashingMachines` | `stop()` wirft nie, auch mit 5 dauerhaft crashenden Maschinen |
| `supervisorRecoversThroughStopAndRestart` | Kompletter Stop-Start-Zyklus funktioniert fehlerfrei |
| `concurrentRegisterDeregisterDoesNotCorruptSupervisor` | 4 Threads registrieren/deregistrieren concurrent ohne Exception |

---

## Bekannte Grenzen

| Szenario | Aktuelles Verhalten |
|----------|-------------------|
| `OutOfMemoryError` / `StackOverflowError` | Wird vom `UncaughtExceptionHandler` geloggt, aber kein automatischer Neustart da die JVM-Ressourcenerschöpfung vorliegt |
| Dauerhafte Belt-Fehler nach Watchdog-Neustart | Thread stirbt erneut → Watchdog startet wieder → Endlosschleife mit 1 s Pause (Backpressure durch Thread-Sleep) |
| Null-Referenz im `Tile`-Locking | `Tile.getLock().lock()` würde NPE werfen → Maschinen-Task-Exception wird gefangen und geloggt |
