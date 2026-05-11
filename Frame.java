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
    
    
    ImageIcon backgroundImage = new ImageIcon("bild.png");
    
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
  