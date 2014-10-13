package controlManager;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;

import main.ControlManager;
import main.L3DPoint;


/**
 * An implementation of MouseController, using the java.awt.Robot class.
 * @author Stephen
 *
 */
public class RobotController implements MouseController {

	/**
	 * Factor for conversion of Kinect coordinates (x,y) to screen coordinates
	 */
	private float kinToScrScaler;
	/**
	 * The Robot instance to control the mouse, etc.
	 */
	private Robot robot;
	/**
	 * Pointer to the owning ControlManager
	 */
	private ControlManager ctrlManager;
	/**
	 * The position of the controlling hand in the last update, null being that there was none.
	 */
	private L3DPoint prevHandPoint = null;
	/**
	 * An array of booleans corresponding to the state the mouse is believed to be in.
	 * localmouseStatus[i] = true <=> ith status is the current one of the mouse.
	 * Indices are 0leftMouseButtonPressed, 1MouseButtonleftReleased
	 * When this differs from the ControlManagers, need to change this, and the real mouse's state, to match.
	 */
	private boolean[] localMouseStatus;
	
	/**
	 * Constructor for class. Sets up the Robot instance, and the necessary local variables for controlling the mouse
	 * @param manager - a ControlManager which is assumed to be the one controlling this instance.
	 * @throws Exception - if we fail to create a Robot instance, announce it to caller
	 */
	public RobotController(ControlManager manager) throws Exception{
		ctrlManager = manager; 
		System.out.println("creating RobotController");
		kinToScrScaler = (float) (ctrlManager.screenWidth)/(float)(ctrlManager.kinectHandler.kinectWidth-(2*ctrlManager.margin)); 
		float temp = (float)(ctrlManager.screenHeight)/(float)(ctrlManager.kinectHandler.kinectHeight-(2*ctrlManager.margin));
		if (temp>kinToScrScaler) kinToScrScaler = temp;
		localMouseStatus = new boolean[2];
		localMouseStatus[0] = false;
		localMouseStatus[1] = true;
		try {
			robot = new Robot();
			//throw new AWTException("dummy");//Used for testing the catch and pop-up code
		} catch (AWTException e) {
			System.out.println("Robot creation error.");
			//e.printStackTrace();
			throw new Exception("System will not allow mouse control of the type required (java.awt.Robot).\nAs such, mouse control is not possible at this time.",e.getCause());
	           
		}
		System.out.println("created RobotController");
	}
	
	public void update() {
		//If the mouse is being controlled and has moved, move it
		if ((ctrlManager.controlHandPos != null)&&!ctrlManager.controlHandPos.equals(prevHandPoint)){
			L3DPoint pos = convertToScreen(ctrlManager.controlHandPos);
			setMousePosition((int) pos.x,(int) pos.y);
			prevHandPoint = ctrlManager.controlHandPos;
		}
		//If told to press a button, do so...unless already done
		if (ctrlManager.mouseStatus[0]&&!localMouseStatus[0]){
			robot.mousePress(InputEvent.BUTTON1_MASK);
			System.out.println("Pressed");
			localMouseStatus[0] = true;
			localMouseStatus[1]= false;
		}
		if (ctrlManager.mouseStatus[1]&&!localMouseStatus[1]){
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
			System.out.println("Released");
			localMouseStatus[0] = false;
			localMouseStatus[1]= true;
			}
	}
	
	/**
	 * Tells the robot to move the mouse to the given coordinates, or to the closest point if off screen.
	 * @param x - the x-coordinate to move to
	 * @param y - the y-coordinate to move to
	 */
	private void setMousePosition(int x, int y){
		//This block ensures that the cursor stays on screen
		if (x>ctrlManager.screenWidth) x = ctrlManager.screenWidth;
		if (x<0) x = 0;
		if (y>ctrlManager.screenHeight) y = ctrlManager.screenHeight;
		if (y<0) y = 0;
		
		robot.mouseMove(x, y);
		
	}

	/**
	 * Takes a L3DPoint, assumed to be in the Kinect image's coordinates, and returns the corresponding value wrt the screen. z coordinate is unchanged.
	 * NOTE: if the L3DPoint is not in the part of the image corresponding to the screen, the method will still work, but the point will be off screen, and the calling method must act accordingly.
	 * @param pos - the L3DPoint in Kinect projection coordinates
	 * @return - the corresponding point in screen coordinates
	 */
	private L3DPoint convertToScreen(L3DPoint pos) {
		return new L3DPoint((int) ((pos.x -ctrlManager.margin) * kinToScrScaler),(int) ((pos.y -ctrlManager.margin) * kinToScrScaler),pos.z);
	}
		
}
