package game.integration;

import game.core.GameSupervisor;
import game.logistics.ConveyorBelt;
import game.logistics.TransportRobot;
import game.machine.BaseMachine;
import game.machine.Miner;
import game.machine.Smelter;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MachineStressAndReliabilityTest {

    @Test
    @Tag("load")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void loadTestParallelProductionSessions() throws InterruptedException {
        final int parallelSessions = 3;
        AtomicInteger successfulSessions = new AtomicInteger(0);
        AtomicReference<Throwable> loadFailure = new AtomicReference<>();

        Thread[] workers = new Thread[parallelSessions];
        for (int i = 0; i < parallelSessions; i++) {
            final int seed = i;
            workers[i] = new Thread(() -> {
                try {
                    boolean success = runSession(1400, seed);
                    if (success) {
                        successfulSessions.incrementAndGet();
                    }
                } catch (Throwable t) {
                    loadFailure.compareAndSet(null, t);
                }
            }, "load-session-" + i);
            workers[i].start();
        }

        for (Thread worker : workers) {
            worker.join(7000);
        }

        assertNull(loadFailure.get(), "No exceptions expected in load test workers.");
        assertEquals(parallelSessions, successfulSessions.get(),
                "All parallel load sessions should finish successfully.");
    }

    @Test
    @Tag("stress")
    @Timeout(value = 12, unit = TimeUnit.SECONDS)
    void stressTestRapidMachineRegistrationDeregistration() throws InterruptedException {
        WorldMap map = new WorldMap(30, 30);
        List<BaseMachine> machines = new CopyOnWriteArrayList<>();
        GameSupervisor supervisor = new GameSupervisor(map, machines,
                new CopyOnWriteArrayList<>(), new CopyOnWriteArrayList<>());

        supervisor.start();
        Thread.sleep(120);
        assertTrue(supervisor.getRunning().get());

        AtomicInteger successfulOps = new AtomicInteger(0);
        AtomicReference<Throwable> workerFailure = new AtomicReference<>();

        Thread[] workers = new Thread[4];
        for (int w = 0; w < workers.length; w++) {
            final int workerId = w;
            workers[w] = new Thread(() -> {
                for (int i = 0; i < 20; i++) {
                    if (workerFailure.get() != null) {
                        return;
                    }
                    try {
                        int x = 2 + workerId * 6;
                        int y = 2 + (i % 20);
                        Tile tile = map.getTile(x, y);

                        tile.getLock().lock();
                        try {
                            if (!tile.hasMachine()) {
                                tile.setType(TileType.IRON_DEPOSIT);
                                tile.setMachine(new Miner(tile));
                            }
                        } finally {
                            tile.getLock().unlock();
                        }

                        BaseMachine machine = tile.getMachine();
                        supervisor.registerMachine(machine);
                        Thread.sleep(8);
                        supervisor.deregisterMachine(machine);

                        tile.getLock().lock();
                        try {
                            tile.removeMachine();
                        } finally {
                            tile.getLock().unlock();
                        }
                        successfulOps.incrementAndGet();
                    } catch (Throwable t) {
                        workerFailure.compareAndSet(null, t);
                        return;
                    }
                }
            }, "stress-worker-" + w);
            workers[w].start();
        }

        for (Thread worker : workers) {
            worker.join(6000);
        }

        supervisor.stop();

        assertNull(workerFailure.get(), "No worker exception expected during stress run.");
        assertEquals(80, successfulOps.get(), "All stress register/deregister operations should complete.");
        assertFalse(supervisor.getRunning().get());
    }

    @Test
    @Tag("reliability")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void reliabilityMetricsSSRAndMTBFBaseline() throws InterruptedException {
        final int sessions = 10;
        final long sessionDurationMs = 1200;

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);
        AtomicLong totalUptimeMs = new AtomicLong(0);

        for (int session = 0; session < sessions; session++) {
            long start = System.currentTimeMillis();
            boolean success = runSession(sessionDurationMs, session);
            long uptime = System.currentTimeMillis() - start;
            totalUptimeMs.addAndGet(uptime);

            if (success) {
                successes.incrementAndGet();
            } else {
                failures.incrementAndGet();
            }
        }

        double ssr = (successes.get() * 100.0) / sessions;
        double totalUptimeSeconds = totalUptimeMs.get() / 1000.0;
        double mtbfSeconds = failures.get() == 0
                ? Double.POSITIVE_INFINITY
                : totalUptimeSeconds / failures.get();

        System.out.println("=== Reliability Baseline ===");
        System.out.println("Sessions=" + sessions);
        System.out.println("Successes=" + successes.get());
        System.out.println("Failures=" + failures.get());
        System.out.println("SSR=" + String.format("%.2f%%", ssr));
        if (Double.isInfinite(mtbfSeconds)) {
            System.out.println("MTBF=INF (no failures observed)");
        } else {
            System.out.println("MTBF=" + String.format("%.2fs", mtbfSeconds));
        }

        assertTrue(ssr >= 80.0, "SSR baseline should remain >= 80% for stability trend tracking.");
        assertTrue(totalUptimeSeconds > 0.0, "Total uptime must be positive.");
    }

    private boolean runSession(long durationMs, int seed) throws InterruptedException {
        WorldMap map = new WorldMap(24, 16);
        List<BaseMachine> machines = new CopyOnWriteArrayList<>();
        List<ConveyorBelt> belts = new CopyOnWriteArrayList<>();
        List<TransportRobot> robots = new CopyOnWriteArrayList<>();

        for (int i = 0; i < 3; i++) {
            int baseX = 2 + i * 6;
            int y = 3 + (seed % 2);

            Tile minerTile = map.getTile(baseX, y);
            minerTile.setType(TileType.IRON_DEPOSIT);
            Miner miner = new Miner(minerTile);
            minerTile.setMachine(miner);
            machines.add(miner);

            Tile smelterTile = map.getTile(baseX + 2, y);
            Smelter smelter = new Smelter(smelterTile);
            smelterTile.setMachine(smelter);
            machines.add(smelter);
        }

        GameSupervisor supervisor = new GameSupervisor(map, machines, belts, robots);

        try {
            supervisor.start();
            long deadline = System.currentTimeMillis() + durationMs;
            int op = 0;

            while (System.currentTimeMillis() < deadline) {
                if (!supervisor.getRunning().get()) {
                    return false;
                }
                int x = 10 + (op % 4);
                int y = 10 + ((op / 2) % 2);
                supervisor.registerBelt(x, y, game.machine.Direction.RIGHT);
                Thread.sleep(50);
                supervisor.deregisterBelt(x, y);
                op++;
            }

            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            supervisor.stop();
        }
    }
}
