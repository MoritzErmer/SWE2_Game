package game.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * Modal tutorial dialog accessible from the main menu.
 * Shows machine icons, describes their functionality, explains the game goal,
 * and warns about the pollution mechanic.
 */
public class TutorialUI extends JDialog {

   private static final int ICON_SIZE = 48;
   private final PixelTextures textures = new PixelTextures(ICON_SIZE);

   public TutorialUI(Frame parent) {
      super(parent, "Tutorial", true);
      setSize(720, 560);
      setLocationRelativeTo(parent);
      setResizable(false);
      setLayout(new BorderLayout());

      TutorialPanel panel = new TutorialPanel();
      JScrollPane scroll = new JScrollPane(panel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scroll.setBorder(null);
      scroll.getVerticalScrollBar().setUnitIncrement(16);
      scroll.getViewport().setBackground(new Color(15, 15, 22));
      add(scroll, BorderLayout.CENTER);

      JPanel footer = new JPanel();
      footer.setBackground(new Color(20, 20, 30));
      JButton closeBtn = new JButton("Schlie\u00DFen");
      closeBtn.setFont(new Font("Monospaced", Font.BOLD, 14));
      closeBtn.setBackground(new Color(60, 80, 140));
      closeBtn.setForeground(Color.WHITE);
      closeBtn.setFocusPainted(false);
      closeBtn.setBorderPainted(false);
      closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      closeBtn.setPreferredSize(new Dimension(160, 36));
      closeBtn.addActionListener(e -> dispose());
      footer.add(closeBtn);
      add(footer, BorderLayout.SOUTH);
   }

   // ===========================================================
   // Inner panel with custom painting
   // ===========================================================

   private class TutorialPanel extends JPanel {

      private static final int PAD = 20;
      private static final int ENTRY_H = 82;

      TutorialPanel() {
         setBackground(new Color(15, 15, 22));
         setPreferredSize(new Dimension(680, 1020));
      }

      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         Graphics2D g2 = (Graphics2D) g;
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

         int y = PAD;
         int xL = PAD;
         int cW = getWidth() - PAD * 2; // content width

         // ── TITEL ──────────────────────────────────────────────────────
         g2.setFont(new Font("Monospaced", Font.BOLD, 26));
         g2.setColor(new Color(255, 200, 50));
         String titleStr = "TUTORIAL";
         g2.drawString(titleStr, (getWidth() - g2.getFontMetrics().stringWidth(titleStr)) / 2, y + 26);
         y += 48;

         // ── SPIELZIEL ──────────────────────────────────────────────────
         y = sectionHeader(g2, "SPIELZIEL", y, xL, cW, new Color(50, 200, 120));
         g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
         g2.setColor(new Color(210, 210, 195));
         y = wrapText(g2, "Repariere die Rakete und entkomme diesem Planeten! "
               + "Sammle Ressourcen, verarbeite sie zu Bauteilen und liefere sie zur Rakete.", xL, y, cW);
         y += 4;
         g2.setFont(new Font("SansSerif", Font.BOLD, 12));
         g2.setColor(new Color(160, 215, 255));
         y = wrapText(g2, "Die Rakete braucht:  Eisenzahnr\u00E4der  \u00B7  Kupferplatten  \u00B7  F\u00F6rderbänder",
               xL, y, cW);
         y += 3;
         g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
         g2.setColor(new Color(150, 170, 195));
         y = wrapText(g2, "Stehe auf dem Raketentile und klicke Rechts, um Items einzuladen.", xL, y, cW);
         y += 16;

         // ── MASCHINEN ──────────────────────────────────────────────────
         y = sectionHeader(g2, "MASCHINEN", y, xL, cW, new Color(255, 200, 50));

         y = machineEntry(g2, "miner", new Color(220, 140, 30), "Miner", xL, y, cW, new String[] {
               "Automatisches Abbauen von Erzvorkommen (Eisen, Kupfer, Kohle).",
               "Nur auf Erzfeldern platzierbar. Treibstoff: Kohle (Rechtsklick).",
               "Richtung mit [R] drehen. Output: Erz \u2192 Förderband / Greifer."
         });
         y = machineEntry(g2, "smelter", new Color(220, 80, 50), "Smelter  (Schmelzofen)", xL, y, cW, new String[] {
               "Schmilzt Eisenerz \u2192 Eisenplatten  und  Kupfererz \u2192 Kupferplatten.",
               "Überall auf freiem Boden platzierbar. Treibstoff: Kohle.",
               "Richtung mit [R] drehen. Output: Metallplatten."
         });
         y = machineEntry(g2, "forge_idle", new Color(195, 160, 110), "Forge  (Schmiede)", xL, y, cW, new String[] {
               "Fertigt Eisenzahnr\u00E4der aus Eisenplatten – ein wichtiges Raketenbauteil!",
               "Überall auf freiem Boden platzierbar. Treibstoff: Kohle.",
               "Richtung mit [R] drehen. Output: Eisenzahnräder."
         });
         y = machineEntry(g2, "grabber", new Color(80, 150, 240), "Greifer", xL, y, cW, new String[] {
               "Transferiert automatisch Items zwischen Maschinen und Förderbändern.",
               "Zielrichtung mit [R] drehen. Kohle wird automatisch aufgefüllt.",
               "Ideales Bindeglied zwischen Maschinen und Transportwegen."
         });
         y = machineEntry(g2, "conveyor_belt", new Color(165, 165, 180), "F\u00F6rderband", xL, y, cW, new String[] {
               "Transportiert Items automatisch in eine Richtung.",
               "Auf freiem Boden platzierbar. Richtung = Blickrichtung beim Platzieren.",
               "Mit [R] drehen. Rechtsklick hebt Items vom Boden auf."
         });
         y += 12;

         // ── GEFAHR: POLLUTION ────────────────────────────────────────
         y = sectionHeader(g2, "\u26A0  GEFAHR: LUFTVERSCHMUTZUNG  \u26A0", y, xL, cW, new Color(255, 90, 65));
         g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
         g2.setColor(new Color(255, 190, 180));
         y = wrapText(g2, "Jede laufende Maschine erzeugt Luftverschmutzung. "
               + "Steigt die Pollution auf 100\u202F%, stirbst du \u2013 und hast verloren!", xL, y, cW);
         y += 4;
         g2.setFont(new Font("SansSerif", Font.BOLD, 12));
         g2.setColor(new Color(255, 145, 120));
         y = wrapText(g2, "Baue nur so viele Maschinen wie unbedingt nötig \u2013 Geschwindigkeit zählt!", xL, y, cW);
         y += 16;

         // ── STEUERUNG ──────────────────────────────────────────────────
         y = sectionHeader(g2, "STEUERUNG", y, xL, cW, new Color(165, 215, 255));
         g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
         String[][] ctrls = {
               { "WASD", "Spieler bewegen" },
               { "Enter (halten)", "Ressource manuell abbauen" },
               { "E", "Inventar \u00F6ffnen / schlie\u00DFen" },
               { "C", "Crafting-Men\u00FC \u00F6ffnen" },
               { "1 \u2013 9", "Hotbar-Slot ausw\u00E4hlen" },
               { "Linksklick", "Item platzieren" },
               { "Rechtsklick", "Maschine bef\u00FCllen / Rakete beladen / Item aufheben" },
               { "R", "Maschine / F\u00F6rderband drehen" },
               { "Q", "Maschine abbauen (ergibt Kit zur\u00FCck)" },
               { "Ctrl+S", "Spielstand speichern" },
         };
         int col2x = xL + 160;
         for (String[] row : ctrls) {
            g2.setColor(new Color(255, 215, 90));
            g2.drawString(row[0], xL + 4, y + 13);
            g2.setColor(new Color(205, 205, 195));
            g2.drawString(row[1], col2x, y + 13);
            y += 17;
         }
      }

      /** Draws a highlighted section header and returns the y below it. */
      private int sectionHeader(Graphics2D g2, String text, int y, int x, int w, Color color) {
         g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
         g2.fillRoundRect(x, y, w, 24, 6, 6);
         g2.setColor(color);
         g2.setFont(new Font("Monospaced", Font.BOLD, 13));
         g2.drawString(text, x + 8, y + 17);
         return y + 28;
      }

      /**
       * Draws a machine entry (background card, icon, name, description).
       * Returns the y below the entry (including a small gap).
       */
      private int machineEntry(Graphics2D g2, String texKey, Color nameCol,
            String name, int x, int y, int w, String[] lines) {
         // Card background
         g2.setColor(new Color(28, 30, 42));
         g2.fillRoundRect(x, y, w, ENTRY_H, 8, 8);
         g2.setColor(new Color(55, 60, 78));
         g2.drawRoundRect(x, y, w, ENTRY_H, 8, 8);

         // Icon – draw machine_bg first for non-belt machines
         int iconY = y + (ENTRY_H - ICON_SIZE) / 2;
         if (!"conveyor_belt".equals(texKey)) {
            BufferedImage bg = textures.get("machine_bg");
            if (bg != null)
               g2.drawImage(bg, x + 8, iconY, null);
         }
         BufferedImage icon = textures.get(texKey);
         if (icon != null)
            g2.drawImage(icon, x + 8, iconY, null);

         // Machine name
         int textX = x + ICON_SIZE + 18;
         g2.setFont(new Font("SansSerif", Font.BOLD, 13));
         g2.setColor(nameCol);
         g2.drawString(name, textX, y + 16);

         // Description lines
         g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
         g2.setColor(new Color(195, 195, 185));
         for (int i = 0; i < lines.length; i++) {
            g2.drawString(lines[i], textX, y + 30 + i * 16);
         }

         return y + ENTRY_H + 6;
      }

      /** Word-wrapping text draw. Returns the y below the last line. */
      private int wrapText(Graphics2D g2, String text, int x, int y, int maxW) {
         FontMetrics fm = g2.getFontMetrics();
         int lineH = fm.getHeight();
         String[] words = text.split(" ");
         StringBuilder sb = new StringBuilder();
         for (String word : words) {
            String test = sb.length() == 0 ? word : sb + " " + word;
            if (fm.stringWidth(test) > maxW && sb.length() > 0) {
               g2.drawString(sb.toString(), x, y + fm.getAscent());
               y += lineH;
               sb = new StringBuilder(word);
            } else {
               sb = new StringBuilder(test);
            }
         }
         if (sb.length() > 0) {
            g2.drawString(sb.toString(), x, y + fm.getAscent());
            y += lineH;
         }
         return y;
      }
   }
}
