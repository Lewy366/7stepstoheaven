
public class LevelChanger {  
  public double currentlevel = 1;           
  
  public LevelChanger() {    
    nextlevel();    
  }  

  public void nextlevel() {    
   //aufrunden weil komputer glitch hat bei doubles
    this.currentlevel = Math.round(this.currentlevel * 10.0) / 10.0;

    
    if (this.currentlevel % 1 == 0) {      
      this.currentlevel = this.currentlevel + 0.1;    
    } else {    
      this.currentlevel = this.currentlevel + 0.9;
    }    
    
    System.out.println("Level:" + " " + this.currentlevel);
  }
}



  