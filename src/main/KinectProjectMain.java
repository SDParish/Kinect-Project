package main;

import subjectManager.DemoSubject;
import subjectManager.ToolBarSM;
import controlManager.MyGestures;
import kinectGUI.GUI;
import kinectHandler.OpenNIHandler;

/**
 * The project's main class, called to create an instance of the application, and creates the top level structure of the project.
 * Also contains the main run loop, which continuously cycles round, updating each section in turn.
 * @author Stephen
 *
 */
public class KinectProjectMain implements Runnable {
	
	/**
	 * Boolean value, while true, run() will update and process Kinect input. Default is false, which also covers when there is no Kinect.
	 */
	protected boolean kinectOn = false;
	/**
	 * The instance of a KinectHandler being used.
	 */
	protected KinectHandler kinectHandler;
	/**
	 * The instance of a KinectGUI being used.
	 */
	protected KinectGUI kinectGUI;
	/**
	 * The instance of a ControlManager being used.
	 */
	protected ControlManager ctrlManager;
	/**
	 * A boolean array that, in the notation used by the ctrlManager, says all buttons are released/not being used by the Kinect code.
	 */
	private boolean[] defaultMouseStatus;
	
	/**
	 * The SubjectManager being used.
	 */
	protected ToolBarSM subjectManager;
	/**
	 * The scale that the visual output will be displayed at. Here so can be changed externally. To be applied, Kinect must be restarted
	 */
	public float imageScale = 1f;
	/**
	 * The Thread that will run the run() method. Exists here in case owning program wants to check its details.
	 */
	protected Thread kinectThread = null;
	/**
	 * Constructor for class. Takes the subject and passes it to a new instance of SubjectManager. Then sets the variables for the other classes to null.
	 */
	public KinectProjectMain(KinectSubject subject){
		subjectManager = new ToolBarSM(this, subject);   
		kinectGUI = null;
		ctrlManager = null;
		kinectHandler = null;
		System.out.println("created kinectProjectMain");
	}
	/**
	 * Creates and initialises the classes to manage the Kinect data.
	 * @throws Exception - there has been an Exception in one of the called methods, so send it on so user is aware.
	 */
	protected void setupKinect() throws Exception{
		kinectHandler = new OpenNIHandler();
		ctrlManager = new MyGestures(kinectHandler, subjectManager);
		defaultMouseStatus = ctrlManager.mouseStatus;
		kinectGUI = new GUI(this,kinectHandler, ctrlManager, imageScale);
		System.out.println("Set up Kinect code");
	}
	/**
	 * Ends/kills the classes that managed the Kinect data.
	 * Note that this assumes that they exist/were created by setupKinect().
	 */
	protected void closeKinect(){
		System.out.println("Closing Kinect");
		kinectHandler.endSession();//Close the kinect driver software/middleware
		kinectGUI.closeWindow();//Need to actively remove the frame containing the kinectGUI output
		kinectGUI = null;
		ctrlManager = null;
		kinectHandler = null;
		System.gc();//later try without
		//Need to reset the toolBar in subjectManager
		subjectManager.displayActive(new int[0], new boolean[0]);
		subjectManager.refeshMouseStatus(defaultMouseStatus);
		System.out.println("Kinect closed");
		
	}
	/**
	 * The run method, called by the Thread created when the Kinect is turned on (by the program). Should only be called when the setupKinect() method has been called since the last time the closeKinect() method has been called.
	 * While kinectOn is true, will call the update methods for the classes that handle the Kinect data. Once kinectOn goes false, will close/dispose of the Kinect data handling classes before finishing/returning (by calling closeKinect()).
	 * Done like this to prevent 'race errors' i.e. the Kinect is not shut down mid update loop.
	 */
	public void run() {
		System.out.println("Starting run");
		while ((kinectOn)){
			updateAll();
		}
		closeKinect();
		System.out.println("Closing run");			
	}
	
	/**
	 * Turns on the Kinect - starts the code to access data from the Kinect, and that which uses it, then leaves it waiting to be activated.
	 * Return false if the Kinect is already on.
	 * @throws Exception - The Kinect code setup failed for some reason, so forward it on to be shown to the user
	 */
	public boolean turnOnKinect() throws Exception{
		subjectManager.setOnOffStatus(true);		
		//if called when connect is already on, do nothing
		if (kinectHandler != null){
			return false;
		}
		setupKinect();
		kinectOn = true;
		//Now start a run thread
		kinectThread = new Thread(this,"kinectThread");
		kinectThread.setDaemon(false);
		kinectThread.start();
		return true;
	}
	/**
	 * Turns off the Kinect - deactivates the Kinect code, and, based on the hypothesis that the kinect was already running, causes the end of the classes.
	 * Return false if the Kinect is already off.
	 */
	public boolean turnOffKinect(){
		subjectManager.setOnOffStatus(false);
		//if called when the Kinect has not been turned on, do nothing.
		if (kinectHandler == null) return false;
		//else disable and stop the Kinect
		kinectOn = false;
		//Need not actively do anything else - the run method will automatically close the Kinect after the current update loop ends and the thread will then finish as the run() method ends
		return true;
	}

	/**
	 * Calls the update method for each of the instances.
	 * If anything goes wrong, show the relevant error pop up, and once it is closed, turn off the Kinect.
	 */
	private void updateAll() {
		//call all the update methods: (in this order, as each relies on the previous one(s))
		try {
			kinectHandler.update();
			ctrlManager.update();
			kinectGUI.update();
		} catch (Exception e) {
			e.printStackTrace();
			subjectManager.kinectProblem(e.getMessage());
			System.out.println("Error - turning off");
			turnOffKinect();
		}
		
	}
	/**
	 * Provides access the the Thread the Kinect update code is/was running in.
	 * Any attempts to alter the Thread will have unknown consequences, this method is purely to provide access for querying it.
	 * @return
	 */
	public Thread getKinectThread(){
		return kinectThread;
	}
	
	/**
	 * Static main method for starting as a stand-alone application. Creates a DemoSubject, and passes it in as a parameter, then calls run.
	 */
	public static void main(String[] args) {
		System.out.println("Static main");
		KinectProjectMain k = new KinectProjectMain(new DemoSubject());
		k.imageScale = 0.5f;
        System.out.println("Main complete.");
	}
	
}
