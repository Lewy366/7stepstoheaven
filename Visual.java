import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
public class Visual extends JPanel{
  // Generator g1 = new Generator();
  // int[][] dieMap = g1.getMap()  ;
  ArrayList<Rectangle> tiles = new ArrayList<>();
  Enemy enemy = new Enemy(50, 200, 0, 350);  //noch nicht bearbeitet

  public Visual() {
    setOpaque(false);
    setPreferredSize(new Dimension(1920,1080));
    // buildMap();  // Commented out since g1 is no longer initialized
  }
  
  
  void buildMap() {
    tiles.clear();
    // buildMap() references removed since g1 and dieMap are no longer available
  }
  
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    
    
    for (Rectangle tile : tiles) {
      g2.setColor(Color.GRAY);
      g2.fillRect(tile.x, tile.y, tile.width, tile.height);
      g2.setColor(Color.DARK_GRAY);
      g2.drawRect(tile.x, tile.y, tile.width, tile.height);
    }
    enemy.draw(g2);
  }
}