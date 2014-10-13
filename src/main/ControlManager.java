package main;

import java.awt.Toolkit;

/**
 * ControlManager interface, covers all necessary parts, but not how the work is done
 * @author Stephen
 *
 */
public abstract class ControlManager {
	
	//fields
	/**
	 * The width of the screen (Pixels).
	 */
	public int screenWidth;
	/**
	 * The height of the screen (Pixels).
	 */
	public int screenHeight;
	 /**
	  * The instance of (an extension of) the KinectHandler providing the Kinect data.
	  */
	public KinectHandler kinectHandler;
	/**
	 * A point representing the location of the controlling hand. If null, there is no controlling hand.
	 * Exists to provide access for GUI
	 */
	public L3DPoint controlHandPos;
	/**
	 * The size of the (minimum) margin around the projection of the screen onto the Kinect data (Pixels).
	 */
	public int margin;
	/**
	 * The target for any output not sent to the (operating) system as a whole.
	 * Will not be a DemoSubject in general. Will need to define an interface 'Target' with appropriate zoom, move methods.
	 */
	public KinectSubject subject;
	/**
	 * The target for the toolbar data.
	 */
	public SubjectManager subjectManager;
	/**
	 * An array of booleans corresponding to the state the mouse is believed to be in.
	 * mouseStatus[i] = true <=> ith status is the current one of the mouse.
	 * Indices are 0leftMouseButtonPressed, 1MouseButtonleftReleased
	 */
	public boolean[] mouseStatus;
	
	//methods
	/**
	 * Constructor to handle the generic part of the class setup
	 * @param kHandler - the KinectHandler providing the Kinect data
	 * @param subjectManager - the SubjectManager displaying the current Kinect status
	 */
	public ControlManager(KinectHandler kHandler, SubjectManager subjectManager) {
		kinectHandler = kHandler;
		this.subjectManager = subjectManager;
		this.subject = subjectManager.getSubject();
		screenWidth = Toolkit .getDefaultToolkit().getScreenSize().width;
    	screenHeight = Toolkit .getDefaultToolkit().getScreenSize().height;
    	margin = 80;
		controlHandPos = null;	
		setMouseStatus();
	}
	/**
	 * Set the current status of the mouse (as believed by this class).
	 */
	private void setMouseStatus() {
		mouseStatus = new boolean[2];
		mouseStatus[0] = false;
		mouseStatus[1] = true;
		
	}
	/**
	 * The Update method. When called, gets the current data from the KinectHandler being used, and acts upon it.
	 */
	public abstract void update();
	
}
