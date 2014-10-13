package main;

import java.awt.Component;

/**
 * KinectGUI abstract class for the display of data from the Kinect. 
 * @author Stephen
 *
 */
public abstract class KinectGUI extends Component{

	/**
	 * The long serialVersionUID required due to extending Component.
	 */
	private static final long serialVersionUID = 1L;
	//fields
	/**
	 * The width of the screen.
	 */
	protected int screenWidth ;
	/**
	 * The height of the screen.
	 */
	protected  int screenHeight;
	/**
	 * The width of the Kinect's images.
	 */
	 protected int kinectWidth;
	/**
	 * The height of the Kinect's images.
	 */
	protected int kinectHeight;
	 /**
	  * The instance of (an extension of) the KinectHandler providing the Kinect data.
	  */
	protected KinectHandler kinectHandler;
	/**
	 * A float value, representing the scale at which the image will be displayed.
	 * 2  = twice the size, 0.5 half the size, etc.
	 */
	protected float scale;
	/**
	 * The instance of (an extension of) ControlManager using the data. Access is required for displaying action specific data.
	 * For example, this allows the KinectGUI instance to get the position of the controlling hand.
	 */
	protected ControlManager ctrlManager;
	
	//methods
	/**
	 * The Update method. When called, gets the current data from the KinectHandler and ControlManager being used.
	 */
	public abstract void update();
	/**
	 * Closes the window, but not the rest of the program.
	 */
	public abstract void closeWindow();
	
}
