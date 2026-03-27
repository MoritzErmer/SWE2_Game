# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
mvn clean package

# Run game
mvn exec:java@run

# Run all tests
mvn clean test

# Run a single test class
mvn test -Dtest=CraftingManagerTest

# Run tests by tag (load, stress, reliability)
mvn test -Dgroups=stress

# Package as Windows EXE
./scripts/package-exe.ps1
```

## Architecture

This is a 2D real-time automation game (Java/Swing) where the player builds production chains via miners, smelters, conveyor belts, and transport robots. The central challenge is correctness under concurrency.

### Thread Model

Five concurrent execution contexts are always running:

| Thread | Owner | Purpose |
|--------|-------|---------|
| EDT | Swing | UI rendering at 30 FPS |
| `ScheduledExecutorService` (producer pool) | `GameSupervisor` | Ticks each machine every 500ms |
| `LogisticsThread` (single-threaded executor) | `GameSupervisor` | Moves items along conveyor belts |
| CachedThreadPool (robot pool) | `GameSupervisor` | One thread per `TransportRobot` |
| `CollisionHandler` thread | `GameSupervisor` | Polls for robot collisions every 50ms |

`GameSupervisor` owns all executors and is the single point of lifecycle control (start/shutdown).

### Synchronization Strategy

- **Per-tile locking**: Each `Tile` holds a `ReentrantLock`. Locks are never held at the map level.
- **Deadlock prevention**: Multi-tile transfers always acquire locks in `System.identityHashCode` order.
- **Thread-safe collections**: `CopyOnWriteArrayList` for machine/belt/robot lists; `ConcurrentHashMap` for belt indices.
- **Lifecycle flags**: `AtomicBoolean` for running state; `volatile` for player/robot positions.

### Key Packages

- `game.core` — `GameSupervisor` (thread lifecycle), `CollisionHandler`
- `game.world` — `WorldMap` (2D grid), `Tile` (per-cell lock + item storage)
- `game.entity` — `PlayerCharacter`, `ItemStack`, `ItemType`
- `game.machine` — `BaseMachine` + subclasses (`Miner`, `Smelter`, `Grabber`), production strategies (Strategy pattern)
- `game.logistics` — `ConveyorBelt`, `LogisticsThread`, `TransportRobot`
- `game.crafting` — `CraftingManager`, `CraftingRecipe` (8 default recipes)
- `game.ui` — `GameUI` (Swing, 32×32 tile rendering), `PixelTextures`

### Design Patterns

- **Strategy**: `ProductionStrategy` — each machine type (Mining, Smelting, Grabber) has its own strategy implementation.
- **Supervisor**: `GameSupervisor` manages all thread executor lifecycles.
- **Producer-Consumer**: Machines produce into tiles; `LogisticsThread` consumes asynchronously.

### Entry Point

`game.Main` initializes `WorldMap`, `GameSupervisor`, and `GameUI`, then installs a global uncaught exception handler before starting the supervisor.

## Testing

Tests are tagged with `@Tag("load")`, `@Tag("stress")`, or `@Tag("reliability")` for targeted execution. Key test classes:

- `SupervisorLifecycleE2ETest` — validates start/stop without deadlock
- `ProductionPipelineIntegrationTest` — full crafting/machine workflows
- `WorldMapTest` — concurrent item transfer thread safety
- `MachineStressAndReliabilityTest` — load and stress scenarios
