package controlManager;


import java.util.HashMap;

import main.ControlManager;
import main.KinectHandler;
import main.KinectHandler.joint;
import main.SubjectManager;


/**
 * A retrospectively poorly named implementation of ControlManager.
 * @author Stephen
 *
 */
public class MyGestures extends ControlManager { 

	/*Inherited fields
	int screenWidth
	int screenHeight
	int kinectWidth
	int kinectHeight
	int margin
	KinectHandler kinectHandler
	L3DPoint controlHandPos
	DemoSubject subject
	boolean[] mouseStatus
	SubjectManager subjectManager
	*/
	/**
	 * An instance of a MouseController, to manage moving and clicking the mouse.
	 */
	protected MouseController mouseController;
	/**
	 * Stores a representation of each user, in terms of which gestures they are doing.
	 * Also implements the results of these gestures.
	 */
	protected HashMap<Integer, MyUserStorer2> myUsers;
	/**
	 * The time since the last update.
	 */
	protected float updateTime;
	/**
	 * Flag to denote whether the mouse failed to initialise/create.
	 */
	protected boolean noMouse = false;
	
	
	/**
	 * The Constructor for the class. Initialises the various constructs/classes used.
	 * If the mouseController fails to initialise, it will still complete construction, but with no mouse control. It will trigger a pop-up from SubjectManager to alert the user to this.
	 * @param kHandler - the kinectHandler providing the Kinect data/skeleton
	 * @param subjectManager - the subjectManager controlling the display of who's active and the state of the mouse
	 */
	public MyGestures(KinectHandler kHandler, SubjectManager subjectManager){
		super(kHandler, subjectManager);
		try {
			mouseController = new RobotController(this);
		} catch (Exception e) {
			// set so no mouse control
			noMouse = true;
			//need to forward on note that there is no mouse control, but throwing the Exception e cancels creation
			subjectManager.kinectProblem(e.getMessage());
		}
		myUsers = new HashMap<Integer, MyUserStorer2>();
		updateTime =kinectHandler.timeGap;		
	}

	/**
	 * Takes the most recent set of skeleton data from the KinectHandler, along with the list of current users. It then updates any of these that already existed with their new skeletons, creates new constructs for any new users, and forgets any users that are no longer being updated.
	 * Also, if mouse control is working, works out which is the closest active user, and tells everyone they are controlling the mouse.
	 */
	public void update() {
		//get the change in time from the last update
		updateTime =kinectHandler.timeGap; 
		int [] users = kinectHandler.users;
		//establish closet active user here
		int closest = 0;//i.e. some value we know is not a valid user
		float minDist =0;//And some distance that cannot be a valid distance
		if (!noMouse){//i.e. if we have no mouse code, do not work out who is the closest active user
			float currentDist =0;
			for (int j = 0; j < users.length; ++j)
			{
				//check that we are tracking this skeleton i.e. the joints exist
				if (kinectHandler.isSkeletonTracking(users[j])){
					if(myUsers.get(users[j]) != null){//i.e. if the user doesn't exist in our HashMap, it is automatically non-active, so is skipped
						if(myUsers.get(users[j]).gestureMode !=0){//i.e. if this user is active, otherwise we skip him/her
							if (minDist ==0){//initial state, no user.
								closest = users[j];
								minDist = kinectHandler.skeletonsReal.get(closest).get(joint.TORSO).z;
							}else{//there is a user, but is this one closer
								currentDist = kinectHandler.skeletonsReal.get(users[j]).get(joint.TORSO).z;
								if (currentDist<minDist){
									minDist = currentDist;
									closest = users[j];
								}
							}	
						}
					}	
				}
			}
		}
		boolean[] active = new boolean[users.length];
		if (minDist ==0) controlHandPos = null;//set so no hand is controlling the mouse if there are no active users. (if there was an active user, minDist would have been overwritten to a non-zero value)
		HashMap<Integer, MyUserStorer2> temp = new HashMap<Integer, MyUserStorer2>();
		for (int j = 0; j < users.length; ++j)
		{
			active[j] = false;
			//check that we are tracking this skeleton i.e. the joints exist
			if (kinectHandler.isSkeletonTracking(users[j])){
				if (myUsers.containsKey(users[j])){
					myUsers.get(users[j]).update(closest);
					temp.put(users[j], myUsers.get(users[j]));
				}else{
					MyUserStorer2 newUser = new MyUserStorer2(this, users[j]);
					newUser.update(closest);
					temp.put(users[j], newUser );
				}	
				active[j] = temp.get(users[j]).getActive();
			}
		}
		//not the neatest (or most space efficient) way, but removes all old users
		myUsers = temp;
		//If it is not the case that the mouseController failed to create, update it
		if (!noMouse) mouseController.update();
		//pass on current mouse status
		subjectManager.refeshMouseStatus(mouseStatus);
		//pass on data to show which users are active
		subjectManager.displayActive(users, active);
		//if a MyUser is the closest one, then they check mouse methods too, and forward on if one happens
	}


}
