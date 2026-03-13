package game;

import game.core.GameSupervisor;
import game.entity.PlayerCharacter;
import game.logistics.ConveyorBelt;
import game.logistics.TransportRobot;
import game.machine.BaseMachine;
import game.ui.GameUI;
import game.world.WorldMap;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

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
      // --- Welt erstellen ---
      final int MAP_WIDTH = 120;
      final int MAP_HEIGHT = 80;
      WorldMap map = new WorldMap(MAP_WIDTH, MAP_HEIGHT);
      map.generateResources(0.15); // 15% der Tiles sind Ressourcen

      // --- Spieler erstellen ---
      PlayerCharacter player = new PlayerCharacter(MAP_WIDTH / 2, MAP_HEIGHT / 2);

      // --- Maschinen, Belts, Roboter (initial leer, werden im Spiel platziert) ---
      List<BaseMachine> machines = new ArrayList<>();
      List<ConveyorBelt> belts = new ArrayList<>();
      List<TransportRobot> robots = new ArrayList<>();

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
}
