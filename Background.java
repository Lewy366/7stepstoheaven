import java.awt.*;
import javax.swing.*;

public class Background extends JPanel {

    private Image backgroundImage;

    public Background() {
        ImageIcon icon = new ImageIcon("e.png");
        backgroundImage = icon.getImage();
        setLayout(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }
}
