import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new SpielUI());
            
    
        JFrame frame = new JFrame("Platformer 2");
    
        frame.setSize(1920, 1080);
    frame.setUndecorated(true);
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(game);
    
    
    // Hintergrund
        Hintergrund background = new Hintergrund();
        background.setBounds(0, 0, 1920, 1080);
        Generator Plattform =  new Generator();
    
        frame.setContentPane(background);
        frame.setVisible(true);
     
        Spieler spieler = new Spieler(Plattform);
        spieler.setBounds(0, 0, 1920, 1080);
        spieler.setOpaque(false); // wichtig!
    
    // Spieler auf Hintergrund legen
        background.add(spieler);
    
    // Fokus für Tastatur
        spieler.requestFocusInWindow();
    
    
    
    }
}
 
