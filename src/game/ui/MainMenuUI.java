package game.ui;

import game.GameMode;
import game.save.GameSaveState;
import game.save.SaveManager;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main menu shown before the game starts.
 * Offers three choices: normal play, creative mode, or load a saved game.
 * Uses CompletableFuture so the caller can block on .get() from a non-EDT
 * thread.
 */
public class MainMenuUI extends JFrame {

   private final CompletableFuture<MenuResult> result = new CompletableFuture<>();

   public record MenuResult(GameMode mode, Optional<GameSaveState> saveState) {
   }

   public MainMenuUI() {
      super("FactoryGame");
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setSize(600, 415);
      setLocationRelativeTo(null);
      setResizable(false);

      JPanel panel = new JPanel() {
         @Override
         protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(20, 20, 30));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 200, 50));
            g.setFont(new Font("Monospaced", Font.BOLD, 36));
            FontMetrics fm = g.getFontMetrics();
            String title = "FACTORY GAME";
            g.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 88);
            g.setColor(new Color(150, 150, 150));
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            String sub = "2D Automation Game";
            fm = g.getFontMetrics();
            g.drawString(sub, (getWidth() - fm.stringWidth(sub)) / 2, 116);
         }
      };
      panel.setLayout(null);

      JButton playBtn = createMenuButton("Spielen", new Color(60, 160, 60));
      playBtn.setBounds(175, 148, 250, 46);
      playBtn.addActionListener(e -> {
         dispose();
         result.complete(new MenuResult(GameMode.NORMAL, Optional.empty()));
      });

      JButton creativeBtn = createMenuButton("Kreativmodus", new Color(60, 100, 200));
      creativeBtn.setBounds(175, 206, 250, 46);
      creativeBtn.addActionListener(e -> {
         dispose();
         result.complete(new MenuResult(GameMode.CREATIVE, Optional.empty()));
      });

      JButton loadBtn = createMenuButton("Laden", new Color(140, 80, 20));
      loadBtn.setBounds(175, 264, 250, 46);
      loadBtn.addActionListener(e -> {
         Optional<GameSaveState> save = SaveManager.load();
         if (save.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kein Spielstand gefunden.", "Laden",
                  JOptionPane.WARNING_MESSAGE);
            return;
         }
         GameMode savedMode = GameMode.NORMAL;
         try {
            savedMode = GameMode.valueOf(save.get().gameMode);
         } catch (Exception ignored) {
         }
         dispose();
         result.complete(new MenuResult(savedMode, save));
      });

      JButton tutorialBtn = createMenuButton("Tutorial", new Color(70, 55, 130));
      tutorialBtn.setBounds(175, 326, 250, 46);
      tutorialBtn.addActionListener(e -> {
         TutorialUI tutorial = new TutorialUI(this);
         tutorial.setVisible(true);
      });

      panel.add(playBtn);
      panel.add(creativeBtn);
      panel.add(loadBtn);
      panel.add(tutorialBtn);
      setContentPane(panel);
   }

   private JButton createMenuButton(String text, Color color) {
      JButton btn = new JButton(text);
      btn.setFont(new Font("Monospaced", Font.BOLD, 16));
      btn.setBackground(color);
      btn.setForeground(Color.WHITE);
      btn.setFocusPainted(false);
      btn.setBorderPainted(false);
      btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      return btn;
   }

   public CompletableFuture<MenuResult> showMenu() {
      setVisible(true);
      return result;
   }
}
