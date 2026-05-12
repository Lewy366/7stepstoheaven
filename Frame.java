import java.awt.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Frame {
  public static void frame(){
    JFrame frame = new JFrame();
    frame.setSize (1920,1080);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    JPanel panel = new JPanel();
    frame.add(panel);
    
    String imagePath = "bild.png";
    java.io.File imageFile = new java.io.File(imagePath);
    if (!imageFile.exists()) {
        imagePath = System.getProperty("user.dir") + "/bild.png";
    }
    ImageIcon backgroundImage = new ImageIcon(imagePath);
    
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    
    Image scaledImage = backgroundImage.getImage().getScaledInstance(
    screenSize.width, screenSize.height, Image.SCALE_SMOOTH
    );
    
    ImageIcon scaledIcon = new ImageIcon(scaledImage);
    
    JLabel background = new JLabel(scaledIcon);
    background.setLayout(new FlowLayout());
    
    frame.setContentPane(background);
    
    frame.setVisible(true);
  }
}
  