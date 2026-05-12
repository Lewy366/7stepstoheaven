import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class SpielUI extends JFrame {

    // --- Farben ---
    private static final Color BG_DARK     = new Color(10, 10, 18);
    private static final Color ACCENT_BLUE = new Color(58, 58, 255);
    private static final Color GREEN_START = new Color(10, 255, 110);
    private static final Color RED_QUIT    = new Color(255, 58, 92);
    private static final Color YELLOW      = new Color(255, 200, 0);
    private static final Color TEXT_LIGHT  = new Color(232, 232, 255);
    private static final Color TEXT_MUTED  = new Color(106, 106, 154);

    // Color per difficulty level (index matches Schwierigkeit.getStufe()-1)
    private static final Color[] DIFF_COLORS = { GREEN_START, YELLOW, RED_QUIT };

    private Schwierigkeit gewählteSchwierigkeit = Schwierigkeit.MITTEL;

    private CardLayout cardLayout;
    private JPanel     mainPanel;

    // Game-screen labels updated on start
    private Timer  spielTimer;
    private int    seconds = 0;
    private JLabel timerLabel;
    private JLabel gameInfoLabel;
    private JLabel gameSterneLabel;
    private JLabel gameDiffLabel;

    public SpielUI() {
        setTitle("Mein Spiel");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(480, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { zeigeBeendenDialog(); }
        });

        cardLayout = new CardLayout();
        mainPanel  = new JPanel(cardLayout);
        mainPanel.setOpaque(false);

        mainPanel.add(erstelleHauptmenu(),    "MENU");
        mainPanel.add(erstelleSpielansicht(), "SPIEL");

        setContentPane(new HintergrundPanel());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    // ── Hauptmenü ─────────────────────────────────────────────────────────────
    private JPanel erstelleHauptmenu() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        JLabel version = erstelleLabel("VERSION 1.0", 10, TEXT_MUTED, Font.PLAIN);
        JLabel titel   = erstelleLabel("MEIN SPIEL", 28, TEXT_LIGHT, Font.BOLD);
        JLabel sub     = erstelleLabel("Das Abenteuer wartet", 12, TEXT_MUTED, Font.PLAIN);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        titel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Schwierigkeits-Sektion ──
        JLabel sectionLabel = erstelleLabel("SCHWIERIGKEIT", 9, TEXT_MUTED, Font.PLAIN);
        sectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel diffRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        diffRow.setOpaque(false);
        diffRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel beschLabel  = erstelleLabel(gewählteSchwierigkeit.getBeschreibung(), 11, TEXT_MUTED, Font.PLAIN);
        JLabel sterneLabel = erstelleLabel(gewählteSchwierigkeit.getSterne(), 16,
                DIFF_COLORS[gewählteSchwierigkeit.getStufe() - 1], Font.PLAIN);
        JLabel statsLabel  = erstelleLabel(buildStatsText(gewählteSchwierigkeit), 10, TEXT_MUTED, Font.PLAIN);
        beschLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sterneLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ButtonGroup gruppe = new ButtonGroup();
        for (int i = 0; i < Schwierigkeit.values().length; i++) {
            Schwierigkeit s = Schwierigkeit.values()[i];
            Color c = DIFF_COLORS[i];
            JToggleButton tb = erstelleToggleButton(s.getAnzeigeName(), c);
            if (s == gewählteSchwierigkeit) tb.setSelected(true);
            gruppe.add(tb);
            diffRow.add(tb);
            tb.addActionListener(e -> {
                gewählteSchwierigkeit = s;
                beschLabel.setText(s.getBeschreibung());
                sterneLabel.setText(s.getSterne());
                sterneLabel.setForeground(c);
                statsLabel.setText(buildStatsText(s));
            });
        }

        // ── Start / Beenden ──
        JButton btnStart   = erstelleButton("▶  Spiel starten", GREEN_START);
        JButton btnBeenden = erstelleButton("✕  Beenden",        RED_QUIT);
        btnStart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBeenden.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnStart.addActionListener(e -> {
            Color dc = DIFF_COLORS[gewählteSchwierigkeit.getStufe() - 1];
            gameDiffLabel.setText(gewählteSchwierigkeit.getAnzeigeName().toUpperCase());
            gameDiffLabel.setForeground(dc);
            gameSterneLabel.setText(gewählteSchwierigkeit.getSterne());
            gameSterneLabel.setForeground(dc);
            gameInfoLabel.setText(buildStatsText(gewählteSchwierigkeit));
            seconds = 0;
            timerLabel.setText("00:00");
            spielTimer.start();
            cardLayout.show(mainPanel, "SPIEL");
        });
        btnBeenden.addActionListener(e -> zeigeBeendenDialog());

        panel.add(version);
        panel.add(Box.createVerticalStrut(4));
        panel.add(titel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(sub);
        panel.add(Box.createVerticalStrut(26));
        panel.add(erstellePixelDivider());
        panel.add(Box.createVerticalStrut(18));
        panel.add(sectionLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(diffRow);
        panel.add(Box.createVerticalStrut(10));
        panel.add(sterneLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(beschLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(statsLabel);
        panel.add(Box.createVerticalStrut(18));
        panel.add(erstellePixelDivider());
        panel.add(Box.createVerticalStrut(18));
        panel.add(btnStart);
        panel.add(Box.createVerticalStrut(12));
        panel.add(btnBeenden);

        return panel;
    }

    // ── Spielansicht ──────────────────────────────────────────────────────────
    private JPanel erstelleSpielansicht() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

        JLabel laufend = erstelleLabel("— SPIEL LÄUFT —", 10, GREEN_START, Font.PLAIN);
        laufend.setAlignmentX(Component.CENTER_ALIGNMENT);

        timerLabel = erstelleLabel("00:00", 42, TEXT_LIGHT, Font.BOLD);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel zeitInfo = erstelleLabel("SPIELZEIT", 11, TEXT_MUTED, Font.PLAIN);
        zeitInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        spielTimer = new Timer(1000, e -> {
            seconds++;
            timerLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        });

        gameDiffLabel = erstelleLabel("MITTEL", 13, YELLOW, Font.BOLD);
        gameDiffLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        gameSterneLabel = erstelleLabel(Schwierigkeit.MITTEL.getSterne(), 18, YELLOW, Font.PLAIN);
        gameSterneLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        gameInfoLabel = erstelleLabel(buildStatsText(Schwierigkeit.MITTEL), 10, TEXT_MUTED, Font.PLAIN);
        gameInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnStop = erstelleButton("⏹  Spiel beenden", RED_QUIT);
        btnStop.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStop.addActionListener(e -> zeigeBeendenDialog());

        panel.add(laufend);
        panel.add(Box.createVerticalStrut(16));
        panel.add(timerLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(zeitInfo);
        panel.add(Box.createVerticalStrut(26));
        panel.add(erstellePixelDivider());
        panel.add(Box.createVerticalStrut(16));
        panel.add(gameDiffLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(gameSterneLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(gameInfoLabel);
        panel.add(Box.createVerticalStrut(16));
        panel.add(erstellePixelDivider());
        panel.add(Box.createVerticalStrut(26));
        panel.add(btnStop);

        return panel;
    }

    // ── Beenden-Dialog ────────────────────────────────────────────────────────
    private void zeigeBeendenDialog() {
        JDialog dialog = new JDialog(this, "Beenden?", true);
        dialog.setUndecorated(true);
        dialog.setSize(320, 200);
        dialog.setLocationRelativeTo(this);

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
        JButton nein = erstelleButton("Nein", TEXT_MUTED);
        ja.setPreferredSize(new Dimension(110, 44));
        nein.setPreferredSize(new Dimension(110, 44));

        ja.addActionListener(e -> { spielTimer.stop(); dialog.dispose(); dispose(); });
        nein.addActionListener(e -> dialog.dispose());

        btnRow.add(ja);
        btnRow.add(nein);
        dlgPanel.add(frage);
        dlgPanel.add(Box.createVerticalStrut(28));
        dlgPanel.add(btnRow);

        dialog.setContentPane(dlgPanel);
        dialog.setVisible(true);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────
    private String buildStatsText(Schwierigkeit s) {
        return "Leben: " + s.getLeben()
             + "  |  Speed: " + s.getGeschwindigkeit()
             + "  |  Bonus: +" + s.getZeitBonus() + "s";
    }

    private JLabel erstelleLabel(String text, int size, Color farbe, int style) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Monospaced", style, size));
        l.setForeground(farbe);
        return l;
    }

    private JButton erstelleButton(String text, Color farbe) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())
                    g2.setColor(farbe.darker().darker());
                else if (getModel().isRollover())
                    g2.setColor(new Color(farbe.getRed(), farbe.getGreen(), farbe.getBlue(), 28));
                else
                    g2.setColor(new Color(0, 0, 0, 0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
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
        btn.setPreferredSize(new Dimension(220, 46));
        btn.setMaximumSize(new Dimension(220, 46));
        return btn;
    }

    private JToggleButton erstelleToggleButton(String text, Color farbe) {
        JToggleButton tb = new JToggleButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected())
                    g2.setColor(new Color(farbe.getRed(), farbe.getGreen(), farbe.getBlue(), 40));
                else if (getModel().isRollover())
                    g2.setColor(new Color(farbe.getRed(), farbe.getGreen(), farbe.getBlue(), 15));
                else
                    g2.setColor(new Color(0, 0, 0, 0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(isSelected() ? farbe : TEXT_MUTED);
                g2.setStroke(new BasicStroke(isSelected() ? 2f : 1f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tb.setFont(new Font("Monospaced", Font.BOLD, 10));
        tb.setForeground(farbe);
        tb.setOpaque(false);
        tb.setContentAreaFilled(false);
        tb.setBorderPainted(false);
        tb.setFocusPainted(false);
        tb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tb.setPreferredSize(new Dimension(100, 38));
        return tb;
    }

    private JPanel erstellePixelDivider() {
        JPanel d = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(ACCENT_BLUE);
                int x = 0;
                while (x < getWidth()) { g.fillRect(x, 0, 6, 2); x += 10; }
            }
        };
        d.setOpaque(false);
        d.setMaximumSize(new Dimension(180, 4));
        d.setPreferredSize(new Dimension(180, 4));
        d.setAlignmentX(Component.CENTER_ALIGNMENT);
        return d;
    }

    // ── Hintergrund mit Scanlines + Ecken ─────────────────────────────────────
    class HintergrundPanel extends JPanel {
        private Image backgroundImage;

        HintergrundPanel() { 
            setOpaque(true); 
            setBackground(BG_DARK);
            loadBackgroundImage();
        }

        private void loadBackgroundImage() {
            try {
                String[] possiblePaths = {
                    "bild.png",
                    "./bild.png",
                    System.getProperty("user.dir") + "/bild.png"
                };
                for (String path : possiblePaths) {
                    java.io.File f = new java.io.File(path);
                    if (f.exists()) {
                        backgroundImage = new ImageIcon(path).getImage();
                        return;
                    }
                }
                System.out.println("Background image not found in: " + java.util.Arrays.toString(possiblePaths));
                backgroundImage = null;
            } catch (Exception e) {
                System.out.println("Error loading background image: " + e.getMessage());
                backgroundImage = null;
            }
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            if (backgroundImage != null) {
                g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }

            g2.setColor(new Color(0, 0, 0, 40));
            for (int y = 0; y < getHeight(); y += 4) g2.drawLine(0, y+3, getWidth(), y+3);
            int s = 20, m = 20;
            g2.setColor(ACCENT_BLUE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(m, m, m+s, m);
            g2.drawLine(m, m, m, m+s);
            g2.drawLine(getWidth()-m-s, m, getWidth()-m, m);
            g2.drawLine(getWidth()-m, m, getWidth()-m, m+s);
            g2.drawLine(m, getHeight()-m, m+s, getHeight()-m);
            g2.drawLine(m, getHeight()-m-s, m, getHeight()-m);
            g2.drawLine(getWidth()-m-s, getHeight()-m, getWidth()-m, getHeight()-m);
            g2.drawLine(getWidth()-m, getHeight()-m-s, getWidth()-m, getHeight()-m);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpielUI::new);
    }
}
