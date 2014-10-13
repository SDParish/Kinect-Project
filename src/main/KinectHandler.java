package main;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;




/**
 * An abstract class listing all the (possibly) required data fields/methods needed from the Kinect.
 * As listed below, they should be independent of drivers, etc, to allow for the potential use of different ones.
 * @author Stephen
 *
 */
public abstract class KinectHandler {
	/**
	 * The width of the Kinect's images.
	 */
	public int kinectWidth;
	/**
	 * The height of the Kinect's images.
	 */
	public int kinectHeight;
	
	/**
	 * ShortBuffer containing data on user locations. (as in where on depth image they are)
	 */
	public ShortBuffer sceneBuffer;
	/**
	 * ShortBuffer containing depth image data.
	 */
	public ShortBuffer depthBuffer;
	/**
	 * ByteBuffer containing RGB image data.
	 */
	public ByteBuffer imageBuffer;
	/**
	 * An array carrying the indices of all the current users
	 */
	public int[] users;
	/**
	 * Represents the joints, as in OpenNI SkeletonJoint, but outside the OpenNI library.
	 * @author Stephen
	 *
	 */
	public static enum joint { HEAD, NECK, TORSO, WAIST, LEFT_COLLAR, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST, LEFT_HAND, LEFT_FINGER_TIP, RIGHT_COLLAR, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST, RIGHT_HAND, RIGHT_FINGER_TIP, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE, LEFT_FOOT, RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE, RIGHT_FOOT }
	/**
     * A HashMap linking a user - as an Integer - to a HashMap of joints - joints - and positions - L3DPoints
     * Stored in Projective Coordinates - i.e. for screen display
     * An entry is in here iff in skeletonsReal, similarly an entry is in the HashMap for a given user iff it is in the HashMap returned for that user by skeletonsReal
	 */
    public HashMap<Integer, HashMap<joint, L3DPoint>> skeletonsProjective;
   /**
    * A HashMap linking a user - as an Integer - to a HashMap of joints - joints - and positions - L3DPoints
     * Stored in Real Coordinates - i.e. for gesture control
    * An entry is in here iff in skeletonsProjective, similarly an entry is in the HashMap for a given user iff it is in the HashMap returned for that user by skeletonsProjective
	 */
    public HashMap<Integer, HashMap<joint, L3DPoint>> skeletonsReal;
    /**
     * The time at the last update.
     */
    protected long lastTime;
	/**
	 * The time since the last update.
	 */
    public float timeGap;
    	
	//methods - will be others, but they won't be needed from outside
	/**
	 * The update method. When called, gets the latest data from the kinect, and updates the fields.
	 * @throws Exception - if the update fails for some reason
	 */
	public abstract void update() throws Exception;

	/**
	 * Returns true if the identified user's skeleton is being tracked
	 * @param user - the index of the user we are interested in
	 * @return - true iff said user has a skeleton which we are tracking.
	 */
	public abstract boolean isSkeletonTracking(int user);
	/**
	 * Ends the current session - closes off/shuts down the kinect.
	 */
	public abstract void endSession();
}
