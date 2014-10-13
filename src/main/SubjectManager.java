package main;


public abstract class SubjectManager {

	/**
	 * Reference to the Project's main running class, used for forwarding input to the rest of the code.
	 */
	protected KinectProjectMain main;
	/**
	 * The subject/program where the Kinect read gestures will be applied.
	 */
	protected KinectSubject subject;
	/**
	 * Partial constructor for the elements that will be common to any implementation.
	 * @param main2 - The KinectProjectMain running this instance of the Kinect code
	 * @param subject2 - the DemoSubject the Kinect input will be applied to
	 */
	public SubjectManager(KinectProjectMain main2, KinectSubject subject2) {
		this.main = main2;
		this.subject = subject2;
	}
	
	/**
	 * Provides access to the subject - replace by relay methods in this class
	 * @return - the subject
	 */
	public KinectSubject getSubject() {
		return subject;
	}
	/**
	 * Produces a pop up message to explain/inform the user about any issues caused by the Kinect code.
	 */
	public abstract void kinectProblem(String s);
		
	/**
	 * Toggles the button representing the mouse status to the appropriate state.
	 * @param mouseStatus
	 */
	public abstract void refeshMouseStatus(boolean[] mouseStatus);
	/**
	 * Update the buttons showing whether users exist and are active
	 * @param users - an array of the id's of the current users
	 * @param active - an array of the same size as users, which records in each entry whether the corresponding user is active or not
	 */
	public abstract void displayActive(int[] users, boolean[] active);
	
	/**
	 * Provides access to the onOffButton for setting its status
	 * @param bool - the state we want the onOffButton to be in
	 */
	public abstract void setOnOffStatus(boolean bool);

}
