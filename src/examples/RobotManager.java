
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * a simplified version of the AWT RobotManager example
 */
public class RobotManager {

  static class Robot {
    String id;
    
    Robot (String id){
      this.id = id;
    }
    
    String getId(){
      return id;
    }
    
    String processSequence(String sequence){
      System.out.printf("robot %s processing sequence: %s\n", id, sequence);
      return "Ok";
    }
  }
  
  class StatusAcquisitionThread extends Thread {
    boolean done;

    public StatusAcquisitionThread () {
      setDaemon(true);
    }

    public void run () {
      int n = robotList.size();
      Random random = new Random(0);

      while (!done) {
        int idx = random.nextInt(n);
        Robot robot = (Robot) robotList.get(idx);
        setRobotOnline(robot, !isRobotOnline(robot.id));

        try {
          Thread.sleep(3000);
        } catch (InterruptedException ix) {
        }
      }
    }

    public void terminate () {
      done = true;
    }
  }
  
  List<Robot> robotList = new ArrayList<Robot>();
  HashMap<String,Robot> onlineRobots = new HashMap<String,Robot>();
  StatusAcquisitionThread acquisitionThread;

  public RobotManager() {
    robotList.add( new Robot("RATS-1"));
    robotList.add( new Robot("RATS-2"));    
    robotList.add( new Robot("RCAT-1"));
    robotList.add( new Robot("POGO-1"));
    
    for (Robot r : robotList) {
      setRobotOnline(r, true);
    }
  }
  
  public void setRobotOnline (Robot robot, boolean isOnline) {
    if (isOnline) {
      onlineRobots.put(robot.getId(), robot);
    } else {
      onlineRobots.remove(robot.getId());
    }
  }
  
  public boolean isRobotOnline (String robotName) {
    return onlineRobots.containsKey(robotName);
  }
  
  public Robot getOnlineRobot (String robotName) {
    return onlineRobots.get(robotName);
  }
  
  public String sendSequence(Robot robot, String sequence) {    
    return robot.processSequence(sequence); 
  }
  
  void startStatusAcquisitionThread (){
    acquisitionThread = new StatusAcquisitionThread();
    acquisitionThread.start();
  }
  
  void stopStatusAcquisitionThread(){
    acquisitionThread.terminate();
  }
  
  void processInput (){
    String robotName = "POGO-1";
    String sequence = "left; go";
    
    if (isRobotOnline(robotName)){
      Robot robot = getOnlineRobot( robotName);
      String result = robot.processSequence(sequence);
      System.out.printf("sent sequence \"%s\" to robot %s => %s\n", sequence, robotName, result);
    } else {
      System.out.print("robot not online: ");
      System.out.println(robotName);
    }
  }
  
  public static void main (String[] args){
    RobotManager robotManager = new RobotManager();
    robotManager.startStatusAcquisitionThread();
    robotManager.processInput();
    //robotManager.stopStatusAcquisitionThread();
  }
}
