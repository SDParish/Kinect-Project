package kinectHandler;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;


import main.KinectHandler;
import main.L3DPoint;

import org.OpenNI.*;


/**
 * Extension of abstract KinectHandler using OpenNI to drive/get data from the Kinect.
 * A lot of the code is based on/derived from the OpenNI sample projects, in particular UserTracker.
 * @author Stephen
 *
 */
public class OpenNIHandler extends KinectHandler {

	//TODO would like to 'lose user' faster. That is if the skeleton is separated from the (physical) user, would like the old skeleton to stop existing, and so not have the possibility of carrying out actions
	//These classes copied from User Tracker
	class NewUserObserver implements IObserver<UserEventArgs>
	{
		@Override
		public void update(IObservable<UserEventArgs> observable,
				UserEventArgs args)
		{
			System.out.println("New user " + args.getId());
			try
			{
				if (skeletonCap.needPoseForCalibration())
				{
					poseDetectionCap.startPoseDetection(calibPose, args.getId());
				}
				else
				{
					skeletonCap.requestSkeletonCalibration(args.getId(), true);
				}
			} catch (StatusException e)
			{
				e.printStackTrace();
			}
		}
	}
	class LostUserObserver implements IObserver<UserEventArgs>
	{
		@Override
		public void update(IObservable<UserEventArgs> observable,
				UserEventArgs args)
		{
			System.out.println("Lost user " + args.getId());
			joints.remove(args.getId());
			skeletonsProjective.remove(args.getId());
			skeletonsReal.remove(args.getId());
			
		}
	}
	
	class CalibrationCompleteObserver implements IObserver<CalibrationProgressEventArgs>
	{
		@Override
		public void update(IObservable<CalibrationProgressEventArgs> observable,
				CalibrationProgressEventArgs args)
		{
			System.out.println("Calibraion complete: " + args.getStatus());
			try
			{
			if (args.getStatus() == CalibrationProgressStatus.OK)
			{
				System.out.println("starting tracking "  +args.getUser());
					skeletonCap.startTracking(args.getUser());
	                joints.put(new Integer(args.getUser()), new HashMap<SkeletonJoint, SkeletonJointPosition>());
	                skeletonsProjective.put(new Integer(args.getUser()), new HashMap<joint, L3DPoint>());
	                skeletonsReal.put(new Integer(args.getUser()), new HashMap<joint, L3DPoint>());
			}
			else if (args.getStatus() != CalibrationProgressStatus.MANUAL_ABORT)
			{
				if (skeletonCap.needPoseForCalibration())
				{
					poseDetectionCap.startPoseDetection(calibPose, args.getUser());
				}
				else
				{
					skeletonCap.requestSkeletonCalibration(args.getUser(), true);
				}
			}
			} catch (StatusException e)
			{
				e.printStackTrace();
			}
		}
	}
	class PoseDetectedObserver implements IObserver<PoseDetectionEventArgs>
	{
		@Override
		public void update(IObservable<PoseDetectionEventArgs> observable,
				PoseDetectionEventArgs args)
		{
			System.out.println("Pose " + args.getPose() + " detected for " + args.getUser());
			try
			{
				poseDetectionCap.stopPoseDetection(args.getUser());
				skeletonCap.requestSkeletonCalibration(args.getUser(), true);
			} catch (StatusException e)
			{
				e.printStackTrace();
			}
		}
	}
	/*Has fields inherited from KinectHandler:
	int kinectWidth
	int kinectHeight
	ShortBuffer sceneBuffer;
	ShortBuffer depthBuffer;
	ByteBuffer imageBuffer;
	int[] users;
	public static enum joint { HEAD, NECK, TORSO, WAIST, LEFT_COLLAR, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST, LEFT_HAND, LEFT_FINGER_TIP, RIGHT_COLLAR, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST, RIGHT_HAND, RIGHT_FINGER_TIP, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE, LEFT_FOOT, RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE, RIGHT_FOOT }
	HashMap<Integer, HashMap<kinectHandler.OpenNIHandler.joint, L3DPoint>> skeletonsProjective
    HashMap<Integer, HashMap<kinectHandler.OpenNIHandler.joint, L3DPoint>> skeletonsReal
    protected long lastTime;
	float timeGap;
	all are public unless said otherwise
	*/
	/**
	 * The ImageGenerator for this class's context. Provides access to the RGB data.
	 */
	private ImageGenerator imageGen;
	/**
	 * An array of the OpenNI SkeletonJoint enums in the same order as the local ones (by design) to use for conversion.
	 * Conversion by skelJoints[joint.XXXX.ordinal()] = SkeletonJoint.XXXX (in theory)
	 */
	private final SkeletonJoint[] skelJoints = SkeletonJoint.values();
	
	//Below fields copied from the OpenNI UserTracker demo. I added the descriptions
	/**
	 * The name of the xml configuration file.
	 */
	private final String SAMPLE_XML_FILE = "SamplesConfig.xml";
	/**
	 * The ScriptNode, has something to do with configuring the Context from the xml file.
	 */
	private OutArg<ScriptNode> scriptNode;
    /**
     * The Context that controls access to the Kinect. All the Nodes/Generators belong to it. 
     */
	private Context context;
	/**
	 * The DepthGenerator for the Context. Provides access to the depth data/image. 
	 */
	private DepthGenerator depthGen;
	/**
	 * The Context's UserGenerator. Handles recognition and tracking of users. Provides access to the 'overlay' of users over the other images.
	 */
    private UserGenerator userGen;
    /**
     * The UserGenerator's SkeletonCapability. It tracks/maintains the users' skeletons.
     */
    private SkeletonCapability skeletonCap;
   /**
    *The UserGenerator's PoseDetectionCapability. It is used if the SkeletonCapability needs the user to strike a pose to be recognised.
     */
    private PoseDetectionCapability poseDetectionCap;
    /**
     * Stores the calibration pose currently in use by the SkeletonCapability.
     */
    private String calibPose = null;
    /**
     * The HashMap of Users to OpenNI joints, in Projective view. - Do I need this??
     */
    private HashMap<Integer, HashMap<SkeletonJoint, SkeletonJointPosition>> joints;
    

    /**
	 * Constructor for class. Calls methods to set up Kinect and initialise fields.
	 * Abridged/edited from UserTracker, (and added to)
     * @throws Exception - if it fails to create/initialise anything (i.e. something goes wrong)
	 */
	public OpenNIHandler() throws Exception{
		System.out.println("creating openNIHandler");
		try {
			 lastTime =  System.currentTimeMillis();
			 timeGap=0f;
			 scriptNode = new OutArg<ScriptNode>();
            context = Context.createFromXmlFile(SAMPLE_XML_FILE, scriptNode);

            depthGen = DepthGenerator.create(context);
            imageGen = ImageGenerator.create(context);
            userGen = UserGenerator.create(context);
            /*Tells the Depth Generator to act as though it is in the same place as the image Generator
            This means that, when they are superimposed (or the users, based off the Depth data, are added to the RGB)
            the two line up. Without this, had the 'coloured in' user, appearing to 'hover' above the user in the RGB image.
            This gives a large black top and sides border - would like to remove - but would then alter image size, etc
            Note that having the RGB camera act as if it is in the same position as the Depth sensor is not supported.
            */
            depthGen.getAlternativeViewpointCapability().setViewpoint(imageGen);
            
            DepthMetaData depthMD = depthGen.getMetaData();
            kinectWidth = depthMD.getFullXRes();
            kinectHeight = depthMD.getFullYRes();            
            skeletonCap = userGen.getSkeletonCapability();
            poseDetectionCap = userGen.getPoseDetectionCapability();
            
            userGen.getNewUserEvent().addObserver(new NewUserObserver());
            userGen.getLostUserEvent().addObserver(new LostUserObserver());
            skeletonCap.getCalibrationCompleteEvent().addObserver(new CalibrationCompleteObserver());
            poseDetectionCap.getPoseDetectedEvent().addObserver(new PoseDetectedObserver());
            //skeletonCap.setSmoothing(0.3f);
            
            calibPose = skeletonCap.getSkeletonCalibrationPose();
            joints = new HashMap<Integer, HashMap<SkeletonJoint,SkeletonJointPosition>>();
            skeletonsProjective = new HashMap<Integer, HashMap<joint,L3DPoint>>();
            skeletonsReal = new HashMap<Integer, HashMap<joint,L3DPoint>>();
            skeletonCap.setSkeletonProfile(SkeletonProfile.UPPER_BODY);
            users = userGen.getUsers();//initialises users, so non-null
                    
            //Initialise buffers
           	depthBuffer = ShortBuffer.allocate(kinectHeight*kinectWidth);
            sceneBuffer = ShortBuffer.allocate(kinectHeight*kinectWidth);
            imageBuffer = ByteBuffer.allocate(kinectHeight*kinectWidth*3);
                       
           	context.startGeneratingAll();
        } catch (GeneralException e) {
        	e.printStackTrace();//May want this enabled or not
        	throw new Exception("The program failed to create and initialise a Kinect instance. \nPlease check you have the Kinect sensor plugged into both the computer, and the mains. \nAlso please check you have the correct OpenNI Drivers installed.\nIf you have just plugged it in, please wait for a minute, then try again.\nIf none of the above apply, please turn off whole program, wait for a few minutes, and restart.",e.getCause());
        	//\nIf this problem persists, kill the process XnSensorServer... which has the description Prime Sense Device Developement Kit.\nIt is needed when using the Kinect, but can cause problems if it already exists when setting up the Kinect code.
        	//if you fail to let it end naturally/open it in one run of the application, but then start a new run that then uses it because it hasn't terminated -it won't terminate on own
        }
        System.out.println("created OpenNIHandler");
	}
	/**
	 * Method used to check that the two sets of Joint enums were matching.
	 * And when they didn't match, the output was used to see the correct order required.
	 */
	private void testords() {
		System.out.println("TESTING ORD JOINT VALUES");
		joint[] list = joint.values();
		int l = list.length;
		for(int i =0; i<l; i=i+1){
			System.out.print(list[i]);
			System.out.print(" : ");
			System.out.print(list[i].ordinal());
			System.out.print(" : ");
			System.out.print(skelJoints[list[i].ordinal()]);
			System.out.println();
		}
	}
	// This was copied/abridged from User Tracker and then added to 
	@Override
	public void update() throws Exception {
		try {
			  
            context.waitAndUpdateAll();
            long newTime = System.currentTimeMillis();
        	timeGap =(new Long(newTime-lastTime)).floatValue();
        	lastTime = newTime;
        	 
            DepthMetaData depthMD = depthGen.getMetaData();
            SceneMetaData sceneMD = userGen.getUserPixels(0);
            ImageMetaData imageMD = imageGen.getMetaData();
          
            //When depthBuffer, sceneBuffer, imageBuffer (as global variables) were set directly equal to depthMD.getData().createShortBuffer(), etc. it led to a memory leak
            //My code to stop the memory leak             
            depthBuffer.rewind();
            sceneBuffer.rewind();
            imageBuffer.rewind();
            depthBuffer.put(depthMD.getData().createShortBuffer());
            sceneBuffer.put(sceneMD.getData().createShortBuffer());
            imageBuffer.put(imageMD.getData().createByteBuffer());
            //this loop from User Tracker's paint method, with getJoints replacing the method that calls it, and the rest removed 
            try
    		{
    			users = userGen.getUsers();
    			for (int i = 0; i < users.length; ++i)
    			{    		    	
    				if (skeletonCap.isSkeletonTracking(users[i]))
    				{
    					getJoints(users[i]);
    				}    				
    			}
    		} catch (StatusException e)
    		{
    			e.printStackTrace();
    		}
    		users = userGen.getUsers();
        } catch (GeneralException e) {
            e.printStackTrace();
            //Cover a potential cause of problem that is easily remedied
            throw new Exception(e.getMessage()+"\nKinect may have become disconnected from power/device.\nIf so, reconnection before closing this message (which will turn off the Kinect) should allow continuation of session after a short delay without terminating program.\nOtherwise, to get it working again, please turn off whole program, wait for a few minutes, and restart.",e.getCause());
        }		
	}

	//These taken straight from UserTracker, but then adjusted for joint conversion
	/**
	 * Calls the OpenNI method to get the user's joint j's current position, and updating the local list.
	 * @param user - the user whose joint is being updated
	 * @param j - the joint being updated
	 * @throws StatusException
	 */
	private void getJoint(int user, joint j) throws StatusException
    {
        SkeletonJointPosition pos = skeletonCap.getSkeletonJointPosition(user, skelJoints[j.ordinal()]);
		if ((pos.getPosition().getZ() != 0)&&( pos.getConfidence()!=0))//changed - added second argument
		{
			joints.get(user).put(skelJoints[j.ordinal()], new SkeletonJointPosition(depthGen.convertRealWorldToProjective(pos.getPosition()), pos.getConfidence()));
			Point3D pt = depthGen.convertRealWorldToProjective(pos.getPosition());
			skeletonsProjective.get(user).put(j, new L3DPoint(pt.getX(), pt.getY(), pt.getZ()));
			skeletonsReal.get(user).put(j, new L3DPoint(pos.getPosition().getX(), pos.getPosition().getY(), pos.getPosition().getZ()));
		}
		else
		{
			if (!skeletonsProjective.get(user).containsKey(j)){//Assume that one skeleton contains a joint iff all do
				joints.get(user).put(skelJoints[j.ordinal()], new SkeletonJointPosition(new Point3D(), 0));
				skeletonsProjective.get(user).put(j, new L3DPoint());
				skeletonsReal.get(user).put(j, new L3DPoint());
			}
		}
    }
	/**
	 * Updates the user's joints (i.e. their skeleton) to their new positions (from the latest Kinect data).
	 * @param user - the user whose joints are being updated
	 * @throws StatusException
	 */
    private void getJoints(int user) throws StatusException
    {	//may want to add code to prevent hand-elbow swapping
    	getJoint(user, joint.HEAD);
    	getJoint(user, joint.NECK);
    	
    	getJoint(user, joint.LEFT_SHOULDER);
    	getJoint(user, joint.LEFT_ELBOW);
    	getJoint(user, joint.LEFT_HAND);
    	
    	getJoint(user, joint.RIGHT_SHOULDER);
    	getJoint(user, joint.RIGHT_ELBOW);
    	getJoint(user, joint.RIGHT_HAND);

    	getJoint(user, joint.TORSO);
    	//Commented out as have swapped to UPPER_BODY only. Left in case want to revert back to ALL.
    	/*getJoint(user, joint.LEFT_HIP);
        getJoint(user, joint.LEFT_KNEE);
        getJoint(user, joint.LEFT_FOOT);

    	getJoint(user, joint.RIGHT_HIP);
        getJoint(user, joint.RIGHT_KNEE);
        getJoint(user, joint.RIGHT_FOOT);*/

    }
	@Override
	public boolean isSkeletonTracking(int user) {
		//try doing only if user on screen??
		return skeletonCap.isSkeletonTracking(user);
	}
	@Override
	public void endSession() {
		try {
			context.stopGeneratingAll();
		} catch (StatusException e) {
			System.out.println("Error with stop generating");
			e.printStackTrace();
		}
		scriptNode.value.dispose();
		poseDetectionCap.dispose();
		skeletonCap.dispose();
		userGen.dispose();
		imageGen.dispose();
		depthGen.dispose();
		context.release();
		context.dispose();
		}
}
