import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import javax.swing.*;
public class MainGame {
  public static void main(String[] args) {
    Visual visual = new Visual();

    // Spieler erstellen
    Spieler spieler = new Spieler(visual);

    // OverlayLayout: stapelt Panels übereinander
    visual.setLayout(new OverlayLayout(visual));

    // Overlay-Panel: transparent, zeichnet nur den Spieler
    JPanel overlayPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            spieler.draw(g);
        }
    };
    overlayPanel.setOpaque(false);         // transparent!
    overlayPanel.setPreferredSize(new Dimension(1920, 1080));

    visual.add(overlayPanel);              // Overlay über die Tiles legen

    // --- Rest bleibt wie gehabt ---
    JFrame spiel = new JFrame("7Steps2Heaven");
    spiel.setSize(1900, 1000);
    spiel.setExtendedState(JFrame.MAXIMIZED_BOTH);
    spiel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    ImageIcon hintergrundBild = new ImageIcon("1.png");
    JLabel background = new JLabel(hintergrundBild);
    background.setLayout(new FlowLayout());
    background.add(visual);
    spiel.setContentPane(background);
    
    // KeyListener registrieren
    spiel.addKeyListener(spieler.keyAdapter);
    spiel.setFocusable(true);

    spiel.setVisible(true);

    // Timer: update + repaint
    Timer timer = new Timer(16, e -> {
    spieler.update();
    visual.enemy.update(visual.tiles, spieler);  // ← spieler hinzufügen
    overlayPanel.repaint();
});
    timer.start();
}
  
}