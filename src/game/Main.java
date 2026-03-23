package game;

import game.core.GameSupervisor;
import game.entity.PlayerCharacter;
import game.logistics.ConveyorBelt;
import game.logistics.TransportRobot;
import game.machine.BaseMachine;
import game.ui.GameUI;
import game.world.WorldMap;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hauptklasse des Spiels. Initialisiert alle Komponenten und startet die
 * Threads.
 *
 * Thread-Architektur:
 * - UI-Thread (EDT): Java Swing Rendering, FPS-begrenzt.
 * - Producer-Thread-Pool: Jede Maschine hat einen periodischen Task
 * (ScheduledExecutorService).
 * - Logistics-Thread: Globaler asynchroner Thread für Fließband-Transport.
 * - Robot-Threads: Jeder TransportRobot als eigenständiger Thread
 * (CachedThreadPool).
 * - Collision-Thread: Dedizierter Thread für Kollisionsprüfung.
 */
public class Main {

   public static void main(String[] args) {
      installGlobalErrorHandler();
      try {
         runGame();
      } catch (Throwable t) {
         handleFatalError("Fatal error during startup", t);
         System.exit(1);
      }
   }

   private static void runGame() {
      // --- Welt erstellen ---
      final int MAP_WIDTH = 120;
      final int MAP_HEIGHT = 80;
      WorldMap map = new WorldMap(MAP_WIDTH, MAP_HEIGHT);
      map.generateResources(0.15); // 15% der Tiles sind Ressourcen

      // --- Spieler erstellen ---
      PlayerCharacter player = new PlayerCharacter(MAP_WIDTH / 2, MAP_HEIGHT / 2);

      // --- Maschinen, Belts, Roboter (initial leer, werden im Spiel platziert) ---
      List<BaseMachine> machines = new CopyOnWriteArrayList<>();
      List<ConveyorBelt> belts = new CopyOnWriteArrayList<>();
      List<TransportRobot> robots = new CopyOnWriteArrayList<>();

      // --- Game Supervisor erstellen ---
      GameSupervisor supervisor = new GameSupervisor(map, machines, belts, robots);

      // --- UI im EDT starten ---
      SwingUtilities.invokeLater(() -> {
         GameUI ui = new GameUI(map, player);

         // Callback: Wenn eine Maschine im UI platziert wird, beim Supervisor
         // registrieren
         ui.setOnMachinePlaced(machine -> {
            supervisor.registerMachine(machine);
            System.out.println("[Main] Maschine registriert: " + machine.getName());
         });

         // Callback: Wenn eine Maschine de-konstruiert wird, beim Supervisor abmelden
         ui.setOnMachineRemoved(machine -> {
            supervisor.deregisterMachine(machine);
            System.out.println("[Main] Maschine abgemeldet: " + machine.getName());
         });

         ui.setOnBeltPlaced(placement -> {
            supervisor.registerBelt(placement.getX(), placement.getY(), placement.getDirection());
            System.out.println("[Main] Belt registriert bei (" + placement.getX() + "," + placement.getY() + ")");
         });

         ui.setOnBeltRemoved((x, y) -> {
            supervisor.deregisterBelt(x, y);
            System.out.println("[Main] Belt abgemeldet bei (" + x + "," + y + ")");
         });

         // Shutdown-Hook: Sicheres Beenden bei Fenster-Schließen
         ui.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
               supervisor.stop();
            }
         });
      });

      // --- Alle Threads starten ---
      supervisor.start();

      System.out.println("=== 2D Automation Game gestartet ===");
      System.out.println("Steuerung: WASD=Bewegen, 1=Miner, 2=Smelter, E=Item aufheben");
      System.out.println("Karte: " + MAP_WIDTH + "x" + MAP_HEIGHT + " Tiles");
   }

   private static void installGlobalErrorHandler() {
      Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
         String source = "Uncaught exception in thread " + thread.getName();
         handleFatalError(source, throwable);
      });
   }

   private static void handleFatalError(String source, Throwable throwable) {
      String appData = System.getenv("APPDATA");
      Path logDir = (appData == null || appData.isBlank())
            ? Path.of("logs")
            : Path.of(appData, "SWE2-Game");
      Path logFile = logDir.resolve("error.log");

      try {
         Files.createDirectories(logDir);
         String entry = java.time.Instant.now() + " | " + source + System.lineSeparator()
               + throwable + System.lineSeparator();

         for (StackTraceElement element : throwable.getStackTrace()) {
            entry += "    at " + element + System.lineSeparator();
         }
         entry += System.lineSeparator();

         Files.writeString(
               logFile,
               entry,
               StandardOpenOption.CREATE,
               StandardOpenOption.APPEND);
      } catch (IOException ioEx) {
         System.err.println("Failed to write error log: " + ioEx.getMessage());
      }

      System.err.println(source + ": " + throwable.getMessage());
      throwable.printStackTrace(System.err);

      SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
            null,
            "A fatal error occurred. See log: " + logFile.toAbsolutePath(),
            "SWE2 Game Error",
            JOptionPane.ERROR_MESSAGE));
   }
}
