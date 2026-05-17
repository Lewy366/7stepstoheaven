import javax.swing.*;

/**
 * Program entry point.
 * Swing UI work is started on the event dispatch thread so painting and events
 * stay in Swing's single-threaded UI model.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameUI::new);
    }
}
