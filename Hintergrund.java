
import java.awt.*;
import javax.swing.*;

public class Hintergrund extends JPanel {

    private Image backgroundImage;

    public Hintergrund() {
        ImageIcon icon = new ImageIcon("e.png");
        backgroundImage = icon.getImage();
        setLayout(null); // wichtig!
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }
}