import java.awt.*;
import javax.swing.*;

/**
 * Simple reusable panel that paints one image stretched to the panel size.
 * The main game currently uses GameUI.BackgroundPanel for level backgrounds,
 * but this class is useful for any standalone image-backed screen.
 */
public class Background extends JPanel {

    private Image backgroundImage;

    public Background() {
        // Loads a default image from the working directory.
        ImageIcon icon = new ImageIcon("e.png");
        backgroundImage = icon.getImage();
        setLayout(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Stretch the image to whatever size the panel currently has.
        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }
}
