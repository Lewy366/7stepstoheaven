import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Timer;

public class SpielUI extends JFrame {

    // --- Farben ---
    private static final Color BG_DARK      = new Color(10, 10, 18);
    private static final Color ACCENT_BLUE  = new Color(58, 58, 255);
    private static final Color GREEN_START  = new Color(10, 255, 110);
    private static final Color RED_QUIT     = new Color(255, 58, 92);
    private static final Color TEXT_LIGHT   = new Color(232, 232, 255);
    private static final Color TEXT_MUTED   = new Color(106, 106, 154);

    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Timer
    private Timer spielTimer;
    private int seconds = 0;
    private JLabel timerLabel;

    public SpielUI() {
        setTitle("Mein Spiel");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(480, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BG_DARK);

        // Bestätigungsdialog beim Schließen
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                zeigeBeendenDialog();
            }
        });

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setOpaque(false);

        mainPanel.add(erstelleHauptmenu(), "MENU");
        mainPanel.add(erstelleSpielansicht(), "SPIEL");

        setContentPane(new HintergrundPanel());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    // ── Hauptmenü ───────────────────────────────────────────────────────────
    private JPanel erstelleHauptmenu() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(60, 60, 60, 60));

        // Titel
        JLabel version = erstelleLabel("VERSION 1.0", 11, TEXT_MUTED, Font.PLAIN);
        JLabel titel   = erstelleLabel("MEIN SPIEL", 30, TEXT_LIGHT, Font.BOLD);
        JLabel sub     = erstelleLabel("Das Abenteuer wartet", 13, TEXT_MUTED, Font.PLAIN);

        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        titel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Trennlinie (Pixel-Style)
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(ACCENT_BLUE);
                int x = 0;
                while (x < getWidth()) {
                    g2.fillRect(x, 0, 6, 2);
                    x += 10;
                }
            }
        };
        divider.setOpaque(false);
        divider.setMaximumSize(new Dimension(180, 4));
        divider.setPreferredSize(new Dimension(180, 4));
        divider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons
        JButton btnStart  = erstelleButton("▶  Spiel starten", GREEN_START);
        JButton btnBeenden = erstelleButton("✕  Beenden",       RED_QUIT);

        btnStart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBeenden.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnStart.addActionListener(e -> {
            seconds = 0;
            timerLabel.setText("00:00");
            spielTimer.start();
            cardLayout.show(mainPanel, "SPIEL");
        });
        btnBeenden.addActionListener(e -> zeigeBeendenDialog());

        // Zusammenbauen
        panel.add(version);
        panel.add(Box.createVerticalStrut(6));
        panel.add(titel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(sub);
        panel.add(Box.createVerticalStrut(32));
        panel.add(divider);
        panel.add(Box.createVerticalStrut(32));
        panel.add(btnStart);
        panel.add(Box.createVerticalStrut(14));
        panel.add(btnBeenden);

        return panel;
    }

    // ── Spielansicht ────────────────────────────────────────────────────────
    private JPanel erstelleSpielansicht() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(60, 60, 60, 60));

        JLabel laufend = erstelleLabel("— SPIEL LÄUFT —", 10, GREEN_START, Font.PLAIN);
        laufend.setAlignmentX(Component.CENTER_ALIGNMENT);

        timerLabel = erstelleLabel("00:00", 42, TEXT_LIGHT, Font.BOLD);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel info = erstelleLabel("SPIELZEIT", 12, TEXT_MUTED, Font.PLAIN);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Swing-Timer für Spielzeit
        spielTimer = new Timer(1000, e -> {
            seconds++;
            timerLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        });

        JButton btnStop = erstelleButton("⏹  Spiel beenden", RED_QUIT);
        btnStop.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStop.addActionListener(e -> zeigeBeendenDialog());

        panel.add(laufend);
        panel.add(Box.createVerticalStrut(20));
        panel.add(timerLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(info);
        panel.add(Box.createVerticalStrut(48));
        panel.add(btnStop);

        return panel;
    }

    // ── Beenden-Bestätigung ─────────────────────────────────────────────────
    private void zeigeBeendenDialog() {
        // Eigener Dialog im Spiel-Stil
        JDialog dialog = new JDialog(this, "Beenden?", true);
        dialog.setUndecorated(true);
        dialog.setSize(320, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel dlgPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 18, 30));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(RED_QUIT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2, 16, 16));
                g2.dispose();
            }
        };
        dlgPanel.setOpaque(false);
        dlgPanel.setLayout(new BoxLayout(dlgPanel, BoxLayout.Y_AXIS));
        dlgPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 24, 30));

        JLabel frage = erstelleLabel("Spiel wirklich beenden?", 12, TEXT_LIGHT, Font.BOLD);
        frage.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton ja   = erstelleButton("Ja",   RED_QUIT);
        JButton nein = erstelleButton("Nein", new Color(106, 106, 154));
        ja.setPreferredSize(new Dimension(110, 44));
        nein.setPreferredSize(new Dimension(110, 44));

        ja.addActionListener(e -> {
            spielTimer.stop();
            dialog.dispose();
            dispose();
        });
        nein.addActionListener(e -> dialog.dispose());

        btnRow.add(ja);
        btnRow.add(nein);

        dlgPanel.add(frage);
        dlgPanel.add(Box.createVerticalStrut(28));
        dlgPanel.add(btnRow);

        dialog.setContentPane(dlgPanel);
        dialog.setVisible(true);
    }

    // ── Hilfsmethoden ───────────────────────────────────────────────────────
    private JLabel erstelleLabel(String text, int size, Color farbe, int style) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Monospaced", style, size));
        label.setForeground(farbe);
        return label;
    }

    private JButton erstelleButton(String text, Color farbe) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(farbe.darker().darker());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(farbe.getRed(), farbe.getGreen(), farbe.getBlue(), 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.setColor(farbe);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Monospaced", Font.BOLD, 11));
        btn.setForeground(farbe);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(220, 48));
        btn.setMaximumSize(new Dimension(220, 48));
        return btn;
    }

    // ── Hintergrund-Panel mit Scanlines + Ecken ─────────────────────────────
    class HintergrundPanel extends JPanel {
        HintergrundPanel() { setOpaque(true); setBackground(BG_DARK); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            // Scanlines
            g2.setColor(new Color(0, 0, 0, 40));
            for (int y = 0; y < getHeight(); y += 4) {
                g2.drawLine(0, y + 3, getWidth(), y + 3);
            }

            // Ecken
            int s = 20, m = 20;
            g2.setColor(ACCENT_BLUE);
            g2.setStroke(new BasicStroke(2f));
            // oben-links
            g2.drawLine(m, m, m + s, m);
            g2.drawLine(m, m, m, m + s);
            // oben-rechts
            g2.drawLine(getWidth()-m-s, m, getWidth()-m, m);
            g2.drawLine(getWidth()-m, m, getWidth()-m, m + s);
            // unten-links
            g2.drawLine(m, getHeight()-m, m + s, getHeight()-m);
            g2.drawLine(m, getHeight()-m-s, m, getHeight()-m);
            // unten-rechts
            g2.drawLine(getWidth()-m-s, getHeight()-m, getWidth()-m, getHeight()-m);
            g2.drawLine(getWidth()-m, getHeight()-m-s, getWidth()-m, getHeight()-m);

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpielUI::new);
    }
}
