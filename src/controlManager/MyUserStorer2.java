package controlManager;


import main.KinectHandler.joint;
import main.L3DPoint;

/**
 * MyUser, but using time to control gestures
 * This one calculates whether a condition holds in the current update, then uses that to update how long it has held for, and as such are the gesture conditions met?
 * Not much storage, much less work per iteration, but has no explicit access to previous data if needed (i.e. can infer if x has held for y, then exactly y ago it did not hold, then who knows)
 * @author Stephen
 *
 */
public class MyUserStorer2 {

	/*
	 * Feature  Projective Coordinates  Real World Coordinates  
		X-Y Units  		pixels  					millimeters  
		Z Units  		millimeters  				millimeters  
		X-Y Origin  upper-left corner of the FOV 	 center of FOV  
		Z Origin  		sensor's plane  			sensor's plane  
		X Direction 	 left to right  			left to right  
		Y Direction  	top to bottom  				bottom to top  
		Z Direction  	away from sensor  			away from sensor  

	 */
	/**
	 * The id of the use this represents.
	 */
	public int user;
	/**
	 * The MyGestures that owns/created this.
	 */
	private MyGestures owner;
	/**
	 * An array containing the outstanding lag times for each gesture.
	 */
	private float[] lagTimes;
	/**
	 * The default lag to impose on opposing gestures.
	 */
	private float lagValue = 500f;
	/**
	 * The number of different (full) gestures defined. -> the size of lagTimes
	 */
	private int gestureNumber = 13;
	/**
	 * The relevant skeleton joint positions from the last update.
	 */
	private L3DPoint[] oldPositions;
	/**
	 * The relevant skeleton joint positions from this update.
	 */
	private L3DPoint[] newPositions;
	/**
	 * Should be an array of the joint enums to use for conversion.
	 * Conversion by skelJoints[i] = joint.XXXX (in theory) where XXXX is in the ith position below
	 * { HEAD, NECK, TORSO, WAIST, LEFT_COLLAR, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST, LEFT_HAND, LEFT_FINGER_TIP, RIGHT_COLLAR, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST, RIGHT_HAND, RIGHT_FINGER_TIP, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE, LEFT_FOOT, RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE, RIGHT_FOOT }
	 */
	protected final joint[] skelJoints = joint.values();
	/**
	 * The length of skelJoints, or rather the number of possible joints
	 */
	protected final int jointNumber = skelJoints.length;
	/**
	 * The length of each of the ___Current arrays- the number of different flags/conditions/part gestures used by the gestures
	 */
	private int flagNumber = 20;
	/**
	 * The list of the current flag values, denoting whether a part gesture has held for its gestureTimesCurrent value. Order same as for the other ____Current arrays.
	 * Each boolean array is in order: 0leftHandStillX, 1rightHandStillX, 2leftHandStillY, 3rightHandStillY, 4leftHandStill, 5rightHandStill, 6leftHandUp, 7rightHandUp, 8leftHandDown, 9rightHandDown, 10leftHandLeft, 11rightHandLeft, 12leftHandRight, 13rightHandRight, 14leftHandForward, 15rightHandForward, 16leftHandBack, 17rightHandBack, 18RightHandaboveleft, 19righthandbelowleft
     */
	private boolean[] flagsCurrent;
	/**
	 * The list of whether a movement (part gesture) took place in the last update. Order same as for the other ____Current arrays.
	 * Each boolean array is in order: 0leftHandStillX, 1rightHandStillX, 2leftHandStillY, 3rightHandStillY, 4leftHandStill, 5rightHandStill, 6leftHandUp, 7rightHandUp, 8leftHandDown, 9rightHandDown, 10leftHandLeft, 11rightHandLeft, 12leftHandRight, 13rightHandRight, 14leftHandForward, 15rightHandForward, 16leftHandBack, 17rightHandBack, 18RightHandaboveleft, 19righthandbelowleft
     */
	private boolean[] updatesCurrent;
	/**
	 * The list of the current time values - how long an action (part gesture) has been happening for. Order same as for the other ____Current arrays.
	 * Each boolean array is in order: 0leftHandStillX, 1rightHandStillX, 2leftHandStillY, 3rightHandStillY, 4leftHandStill, 5rightHandStill, 6leftHandUp, 7rightHandUp, 8leftHandDown, 9rightHandDown, 10leftHandLeft, 11rightHandLeft, 12leftHandRight, 13rightHandRight, 14leftHandForward, 15rightHandForward, 16leftHandBack, 17rightHandBack, 18RightHandaboveleft, 19righthandbelowleft
     */
	private float[] timesCurrent;
	/**
	 * The list of the times for how long each (part) gesture should take. Order same as for the other ____Current arrays.
	 * Each boolean array is in order: 0leftHandStillX, 1rightHandStillX, 2leftHandStillY, 3rightHandStillY, 4leftHandStill, 5rightHandStill, 6leftHandUp, 7rightHandUp, 8leftHandDown, 9rightHandDown, 10leftHandLeft, 11rightHandLeft, 12leftHandRight, 13rightHandRight, 14leftHandForward, 15rightHandForward, 16leftHandBack, 17rightHandBack, 18RightHandaboveleft, 19righthandbelowleft
     */
	private float[] gestureTimesCurrent;
	//bounds/limits for determining whether a point should count as either still, or as having moved (or neither - it wasn't kept still, but wasn't a deliberate move either)
	/**
	 * Maximum speed to count as being still.
	 */
	private float noSpeed = 0.75f;
	/**
	 * Minimum speed to count as moving.
	 */
	private float yesSpeed = 1.25f;
	/**
	 * Minimum speed to count as clicking/moving fast.
	 */
	private float clickSpeed = 1.5f*yesSpeed;
	//Times for how long a gesture should last before being recognised, and having been recognised, how long until it should be recognised again.
	/**
	 * The standard duration time for a gesture
	 */
	private float gestureTime = 128f;//160f;//
	/**
	 * The duration time for a click.
	 */
	private float clickTime = gestureTime/2f;
	/**
	 * How long the code should wait after recognising a gesture until repeating the result. Applied as lag to self.
	 */
	private float gestureRepeatTime = 32f;//- if 32ms, given use of All updates, and 30FPS, not really need - keep though, in case a better sensor gets used etc.
	/**
	 * Records which mode we are in - i.e. what gestures we are after
	 * 0 = looking only for 'activation' gesture
	 * 1 = navigation/manipulation + mouse control
	 */
	protected int gestureMode = 0;
	 /**
     * Is true iff this user is the one closest to the sensor
     */
	protected boolean amClosest = false;
	/**
	 * The value for how far in front of the torso the hand must be to control the mouse.
	 * The initial value is arbitrary. Is set each time (when closest) to the user's forearm length. 
	 */
	private float threshold = 200;
	/**
	 * Records whether the current skeleton is believed to be a repeat - i.e. the same set of data provided a second time. Happens when a user leaves, but their skeleton stays.
	 * Note that the implicit assumption is that the user cannot stay perfectly still as seen by the Kinect.
	 */
	private boolean repeated = false;
	/**
	 * The time since a previous skeleton was seen.
	 */
	private float skeletonTimeGap = 0f;
	
	
	/**
	 * Constructor, sets up the class.
	 * @param own - the MyGestures that will provide it with updates
	 * @param userNo - the id number of the user this represents.
	 */
	public MyUserStorer2(MyGestures own, int userNo) {
		user = userNo;
		owner = own;
		setupStorage();
		//Note that a MyUser is only ever created if the user has a skeleton
		skeletonTimeGap = owner.updateTime;
	}
	/**
	 * Sets up the various storage arrays and makes them ready for use.
	 */
	private void setupStorage(){
		lagTimes = new float[gestureNumber];
		//just make sure all lags are initially 0
		resetLags();
		//set up list of flag arrays
		timesCurrent = new float[flagNumber];
		gestureTimesCurrent = new float[flagNumber];
		flagsCurrent = new boolean[flagNumber];
		updatesCurrent = new boolean[flagNumber];
		for (int i =0; i<flagNumber; i=i+1){
			timesCurrent[i] = 0f;
			gestureTimesCurrent[i] = gestureTime;
			flagsCurrent[i] = false;
			updatesCurrent[i] = false;
		}
		//Change the ones that want to have different values
		gestureTimesCurrent[14] = clickTime;
		gestureTimesCurrent[15] = clickTime;
		gestureTimesCurrent[16] = clickTime;
		gestureTimesCurrent[17] = clickTime;
		setupNewPositions();
	}
	/**
	 * Clears and resets the newPosistions array.
	 */
	private void setupNewPositions() {
		newPositions = new L3DPoint[jointNumber];
		for(int i = 0; i< jointNumber; i=i+1){
			newPositions[i] = null;
		}
		//Add blank points explicitly for all used joints { HEAD, NECK, TORSO, LEFT_SHOULDER, LEFT_ELBOW, LEFT_HAND, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_HAND, LEFT_HIP, LEFT_KNEE, LEFT_FOOT, RIGHT_HIP, RIGHT_KNEE, RIGHT_FOOT }
		newPositions[joint.RIGHT_HAND.ordinal()]= new L3DPoint();
		newPositions[joint.LEFT_HAND.ordinal()]= new L3DPoint();
		newPositions[joint.TORSO.ordinal()]= new L3DPoint();
		//from now on, can just go from 0 to jointNumber-1, and only act if get non-null output
		
		
	}
	/**
	 * Sets all the lag values to 0.
	 */
	private void resetLags() {
		for (int i =0; i<gestureNumber; i=i+1){
			lagTimes[i] = 0.0f;
		}		
	}
	
	
	/**
	 * Updates the locally stored skeleton data, and whether this user is closest. Then attempts to recognise any gestures.
	 * @param closestUser - the id number of the user currently in control of the mouse
	 */
	public void update(int closestUser) {
		amClosest = (closestUser == user);
		//new data exists by assumption that method is only called when that is the case
		oldPositions = newPositions;
		setupNewPositions();
		for(int i = 0; i< jointNumber; i=i+1){
			L3DPoint newPosition = newPositions[i];
			if (newPosition != null){//i.e. it is one of the joints we are following
				newPositions[i]= owner.kinectHandler.skeletonsReal.get(user).get(skelJoints[i]);
			}
		}
		repeated = isRepeat();		
		skeletonTimeGap =owner.updateTime;
		if (repeated){
			System.out.println("Repeat");
		}
		matchGestures();				
	}

	/**
	 * First updates all the part gesture arrays as required, then tests to see if if any gestures have taken place.
	 */
	private void matchGestures() {
		//update with new data set - can go here as only use current and 1 previous set of data, which will always exist when this is called (even if first previous set is empty).
		calculatePartialBooleans(gestureMode);
		//Check gesture conditions. Up here as needs to happen every iteration to keep times up to date
		calculateCurrentBooleans(gestureMode);
		//decrement lagTimes
		decrementLag(gestureMode);
		//As the activate/deactivate gesture, this is outside the conditional.
		handsForward12();
		if (gestureMode == 1){//The Kinect is actively being used
			//Check for gestures
			apart0();
			together1();
			handUp2();
			handDown3();
			handLeft4();
			handRight5();
			handsUp6();
			handsDown7();
			handsLeft8();
			handsRight9();
				
			if (amClosest){//This user is controlling the mouse
				joint closest = joint.RIGHT_HAND;
				float displacement =0f;
				//if (!flagsCurrent[ ]&&(lagTimes[ ] ==0)){//i.e. if the right hand is not doing a 'move forwards' action, and have not just done one
					displacement = displacementZ(joint.RIGHT_HAND, joint.TORSO);
				//}
				float temp = 0f;
				//if (!flagsCurrent[ ]&&(lagTimes[ ] ==0)){//i.e. if the left hand is not doing a 'move forwards' action, and have not just done one
					temp = displacementZ(joint.LEFT_HAND, joint.TORSO);
				//}
				//the above if's - that is the lag parts, are causing displacement 0 after every click => release
				if (temp > displacement) {
					displacement = temp;
					closest = joint.LEFT_HAND;
				}//thus closest is the hand farthest ahead of the torso, and displacement how far, where hands doing a 'move forwards' don't count
				threshold = distBetween(owner.kinectHandler.skeletonsReal.get(user).get(joint.RIGHT_HAND), owner.kinectHandler.skeletonsReal.get(user).get(joint.RIGHT_ELBOW));
				//System.out.println("User "+user+": Threshold = "+threshold);
				if (displacement < threshold){
					//if hand not far enough in front
					//Release any pressed mouse buttons -
					owner.mouseStatus[0] = false;
					owner.mouseStatus[1] = true;
					owner.controlHandPos = null;					
				}else{//else need to control mouse
					//set new mouse location
					//only change if current location is significantly different (i.e. smoothing the data, this is a very basic attempt) - first check there is a previous location
					if (owner.controlHandPos == null){
						owner.controlHandPos = owner.kinectHandler.skeletonsProjective.get(user).get(closest);
					}else{
						L3DPoint oldPos = owner.controlHandPos;
						L3DPoint newPos = owner.kinectHandler.skeletonsProjective.get(user).get(closest);
						if ((newPos.x-oldPos.x >5)||(newPos.x-oldPos.x <-5)||(newPos.y-oldPos.y >5)||(newPos.y-oldPos.y <-5)){
							owner.controlHandPos = owner.kinectHandler.skeletonsProjective.get(user).get(closest);
						}
					}
					//look for mouse click gestures
					leftPress10(closest);
					leftRelease11(closest);
					}
				}			
			}		
	}
	//This method works, but can lead to temporary swapping of ctrlhand - add conditions to choice of ctrl hand - if 17/18 hold, no change - need lag too
	//without lags, 'works' in sense that a short sharp push will 'click'- so don't need conditions on ctrlhand
	//other hand push forward
	/**
	 * Sees if, while the ctrlHand is controlling the mouse position, the other hand has pushed forwards quickly.
	 * @param ctrlHand - the joint (hand) controlling the mouse position.
	 */
	private void leftPress10(joint ctrlHand) {
		if ((joint.LEFT_HAND.equals(ctrlHand)&&flagsCurrent[4]&&flagsCurrent[15])||(joint.RIGHT_HAND.equals(ctrlHand)&&flagsCurrent[5]&&flagsCurrent[14])){
			owner.mouseStatus[0] = true;
			owner.mouseStatus[1] = false;
			lagTimes[10] = 250f;
			lagTimes[11] = 0f;
		}
	}
	/**
	 * Sees if, while the ctrlHand is controlling the mouse position, the other hand has pulled backwards quickly.
	 * @param ctrlHand - the joint (hand) controlling the mouse position.
	 */
	private void leftRelease11(joint ctrlHand) {
		if ((joint.LEFT_HAND.equals(ctrlHand)&&flagsCurrent[4]&&flagsCurrent[17])||(joint.RIGHT_HAND.equals(ctrlHand)&&flagsCurrent[5]&&flagsCurrent[16])){
			owner.mouseStatus[0] = false;
			owner.mouseStatus[1] = true;
			lagTimes[11] = 250f;
			lagTimes[10] = 0f;
		}
	}
	/**
	 * Using the new data from the update, updates the stored boolean values stating whether the conditions for each (part) gesture were satisfied in that update. Only bother doing the currently used gestures.
	 * @param mode - the mode the user is in. 0 is inactive, 1 active 
	 */
	private void calculatePartialBooleans(int mode) {
		int leftHand = joint.LEFT_HAND.ordinal();
		int rightHand = joint.RIGHT_HAND.ordinal();
		
		//These two outside if clause as are used for activation gesture
		updatesCurrent[14] =(newPositions[leftHand].z<oldPositions[leftHand].z)&&speedComp(skeletonTimeGap, oldPositions[leftHand].z, newPositions[leftHand].z, clickSpeed);
		updatesCurrent[15] =(newPositions[rightHand].z<oldPositions[rightHand].z)&&speedComp(skeletonTimeGap, oldPositions[rightHand].z, newPositions[rightHand].z, clickSpeed);
		
		if (mode ==1){
			//check required condition holds for each update in the last time interval
			updatesCurrent[0] =!(speedComp(skeletonTimeGap, oldPositions[leftHand].x, newPositions[leftHand].x, noSpeed));
			updatesCurrent[1] =!(speedComp(skeletonTimeGap, oldPositions[rightHand].x, newPositions[rightHand].x, noSpeed));
			updatesCurrent[2] =!(speedComp(skeletonTimeGap, oldPositions[leftHand].y, newPositions[leftHand].y, noSpeed));
			updatesCurrent[3] =!(speedComp(skeletonTimeGap, oldPositions[rightHand].y, newPositions[rightHand].y, noSpeed));		
			updatesCurrent[4] =(updatesCurrent[0]&&updatesCurrent[2]&&!(speedComp(skeletonTimeGap, oldPositions[leftHand].z, newPositions[leftHand].z, noSpeed)));
			updatesCurrent[5] =(updatesCurrent[1]&&updatesCurrent[3]&&!(speedComp(skeletonTimeGap, oldPositions[rightHand].z, newPositions[rightHand].z, noSpeed)));
		
			updatesCurrent[6] =(newPositions[leftHand].y>oldPositions[leftHand].y)&&speedComp(skeletonTimeGap, oldPositions[leftHand].y, newPositions[leftHand].y, yesSpeed);
			updatesCurrent[7] =(newPositions[rightHand].y>oldPositions[rightHand].y)&&speedComp(skeletonTimeGap, oldPositions[rightHand].y, newPositions[rightHand].y, yesSpeed);
			updatesCurrent[8] =(newPositions[leftHand].y<oldPositions[leftHand].y)&&speedComp(skeletonTimeGap, oldPositions[leftHand].y, newPositions[leftHand].y, yesSpeed);
			updatesCurrent[9] =(newPositions[rightHand].y<oldPositions[rightHand].y)&&speedComp(skeletonTimeGap, oldPositions[rightHand].y, newPositions[rightHand].y, yesSpeed);
			updatesCurrent[10] =(newPositions[leftHand].x<oldPositions[leftHand].x)&&speedComp(skeletonTimeGap, oldPositions[leftHand].x, newPositions[leftHand].x, yesSpeed);
			updatesCurrent[11] =(newPositions[rightHand].x<oldPositions[rightHand].x)&&speedComp(skeletonTimeGap, oldPositions[rightHand].x, newPositions[rightHand].x, yesSpeed);
			updatesCurrent[12] =(newPositions[leftHand].x>oldPositions[leftHand].x)&&speedComp(skeletonTimeGap, oldPositions[leftHand].x, newPositions[leftHand].x, yesSpeed);			
			updatesCurrent[13] =(newPositions[rightHand].x>oldPositions[rightHand].x)&&speedComp(skeletonTimeGap, oldPositions[rightHand].x, newPositions[rightHand].x, yesSpeed);
		
			updatesCurrent[16] =(newPositions[leftHand].z>oldPositions[leftHand].z)&&speedComp(skeletonTimeGap, oldPositions[leftHand].z, newPositions[leftHand].z, clickSpeed);			
			updatesCurrent[17] =(newPositions[rightHand].z>oldPositions[rightHand].z)&&speedComp(skeletonTimeGap, oldPositions[rightHand].z, newPositions[rightHand].z, clickSpeed);
		
			//note that in the below, there is a small overlap, where both can hold - this is so if you end just below (i.e. overshoot a little by mistake) that still counts
			updatesCurrent[18] = (newPositions[rightHand].y+50)> newPositions[leftHand].y;
			updatesCurrent[19] = newPositions[rightHand].y< (newPositions[leftHand].y+50);
		
			if (repeated){//Probably should drop
				for (int i =0; i<flagNumber; i=i+1){//if the data was a repeat, everything is set true - not ideal, but may work. Do want better solution.
					updatesCurrent[i] = true;
				}
			}
			
		}
	}
	/**
	 * Calculates whether the new skeleton is a repeat of the last one (which implies this wasn't a proper update)
	 * @return - true if the new skeleton was an exact repeat of the last one, false otherwise. (skeleton here meaning only the joints we care about).
	 */
	private boolean isRepeat(){
		for(int i = 0; i< jointNumber; i=i+1){
			L3DPoint newPosition = newPositions[i];
			if (newPosition != null){//i.e. this joint is one we are tracking
				if(newPositions[i].x!=oldPositions[i].x) return false;
				if(newPositions[i].y!=oldPositions[i].y) return false;
				if(newPositions[i].z!=oldPositions[i].z) return false;
			}
		}
		return true;
	}
	/**
	 * Works out the times for how long any part gesture has lasted, and updates the corresponding flags accordingly if they have gone on long enough. Only bother doing the currently used gestures.
	 * @param mode - the mode the user is in. 0 is inactive, 1 active 
	 */
	private void calculateCurrentBooleans(int mode) {
		//if a movement is happening, increment its time duration, else set to 0, and set its flag as to whether it has gone on long enough
		if (mode ==1){
			for (int i =0; i<flagNumber; i=i+1){
				if (updatesCurrent[i]){//happened in last update
					timesCurrent[i] = timesCurrent[i]+skeletonTimeGap;
					flagsCurrent[i] = (timesCurrent[i]>gestureTimesCurrent[i]);
					//System.out.println(" update = "+updatesCurrent[i]+" Posit = "+i+" time = "+ timesCurrent[i]+" bool = "+flagsCurrent[i]);//testing
					}else{//did not happen
						timesCurrent[i] = 0;
						flagsCurrent[i] = false;
						//System.out.println(" update = "+updatesCurrent[i]+" Posit = "+i+" time = "+ timesCurrent[i]+" bool = "+flagsCurrent[i]);//testing
					}
			}
		}else{//mode ==0
			for (int i =14; i<16; i=i+1){//i.e. i is 14 or 15
				if (updatesCurrent[i]){//happened in last update
					timesCurrent[i] = timesCurrent[i]+skeletonTimeGap;
					flagsCurrent[i] = (timesCurrent[i]>gestureTimesCurrent[i]);
					//System.out.println(" update = "+updatesCurrent[i]+" Posit = "+i+" time = "+ timesCurrent[i]+" bool = "+flagsCurrent[i]);//testing
					}else{//did not happen
						timesCurrent[i] = 0;
						flagsCurrent[i] = false;
						//System.out.println(" update = "+updatesCurrent[i]+" Posit = "+i+" time = "+ timesCurrent[i]+" bool = "+flagsCurrent[i]);//testing
					}		
			}
		}
				
	}
	/**
	 * Decrease all positive values in lagTimes by the time taken in the most recent update. Only bother doing the currently used gestures.
	 * @param mode - the mode the user is in. 0 is inactive, 1 active 
	 */
	private void decrementLag(int mode) {
		if (mode ==1){
			for (int i =0; i<gestureNumber; i=i+1){
				if (lagTimes[i]>0){//if it is waiting, decrease the time
					lagTimes[i] = lagTimes[i]-skeletonTimeGap;
					if (lagTimes[i]<0){//if this result is negative, set it to 0
						lagTimes[i] = 0f;
					}
				}
			}
		}else{//mode ==0			
				if (lagTimes[12]>0){//if it is waiting, decrease the time
					lagTimes[12] = lagTimes[12]-skeletonTimeGap;
					if (lagTimes[12]<0){//if this result is negative, set it to 0
						lagTimes[12] = 0f;
					}
				}			
		}
	}
	//want to try and make proportional to size of gesture
	/**
	 * The gesture defined by the user's hands being deliberately moved apart.
	 */
	private void apart0(){
		if (lagTimes[0]!=0) return;//Separate so saves work if !=0
		if ((flagsCurrent[13]&&flagsCurrent[10])/*or in vertical axis*/||(flagsCurrent[19]&&flagsCurrent[6]&&flagsCurrent[9])||(flagsCurrent[18]&&flagsCurrent[7]&&flagsCurrent[8])){
			System.out.println("Apart");
			owner.subject.setZoom(1);
			lagTimes[1] = lagValue;//To prevent 'undoing' the action just done
			lagTimes[0] = (gestureRepeatTime);//To limit the number that can be carried out in a given time interval - a very fast computer would pick up the movement continuing lots of times, even if each is ~15ms
		}
	}
	/**
	 * The gesture defined by the user's hands being deliberately brought together.
	 */
	private void together1(){
		if (lagTimes[1]!=0) return;
		if ((flagsCurrent[11]&&flagsCurrent[12])/* or in vertical axis*/||(flagsCurrent[18]&&flagsCurrent[6]&&flagsCurrent[9])||(flagsCurrent[19]&&flagsCurrent[7]&&flagsCurrent[8])){
			System.out.println("Together");
			owner.subject.setZoom(-1);
			lagTimes[0] = lagValue;
			lagTimes[1] = (gestureRepeatTime);
			}
	}

	/**
	 * The gesture defined by one of the user's hands being still, whilst the other goes up.
	 */
	private void handUp2() {
		if (lagTimes[2]!=0) return;
		if ( (flagsCurrent[7]&&flagsCurrent[4])||(flagsCurrent[6]&&flagsCurrent[5])){
			System.out.println("Up");
			owner.subject.setShift(0,1,0);
			lagTimes[3] = lagValue;
			lagTimes[2] = (gestureRepeatTime);
		}		
	}
	/**
	 * The gesture defined by one of the user's hands being still, whilst the other goes down.
	 */
	private void handDown3() {
		if (lagTimes[3]!=0) return;
		if ((flagsCurrent[9]&&flagsCurrent[4])||(flagsCurrent[8]&&flagsCurrent[5])){
			System.out.println("Down");
			owner.subject.setShift(0,-1,0);
			lagTimes[2] = lagValue;
			lagTimes[3] = (gestureRepeatTime);
		}		
	}
	/**
	 * The gesture defined by one of the user's hands being still, whilst the other goes left.
	 */
	private void handLeft4() {
		if (lagTimes[4]!=0) return;
		if ((flagsCurrent[11]&&flagsCurrent[4])||(flagsCurrent[10]&&flagsCurrent[5])){
			System.out.println("Left");
			owner.subject.setShift(-1,0,0);
			lagTimes[5] = lagValue;
			lagTimes[4] = (gestureRepeatTime);
		}		
	}
	/**
	 * The gesture defined by one of the user's hands being still, whilst the other goes right.
	 */
	private void handRight5() {
		if (lagTimes[5]!=0)	return;
		if ((flagsCurrent[13]&&flagsCurrent[4])||(flagsCurrent[12]&&flagsCurrent[5])){
			System.out.println("Right");
			owner.subject.setShift(1,0,0);
			lagTimes[4] = lagValue;
			lagTimes[5] = (gestureRepeatTime);
		}		
	}
	/**
	 * The gesture defined by both of the user's hands going up.
	 */
	private void handsUp6() {
		if (lagTimes[6]!=0) return;
		if (flagsCurrent[7]&&flagsCurrent[6]){
			System.out.println("Rotate up");
			owner.subject.setRotate(0,1,0);
			lagTimes[7] = lagValue;
			lagTimes[6] = (gestureRepeatTime);
		}		
	}
	/**
	 * The gesture defined by both of the user's hands going down.
	 */
	private void handsDown7() {
		if (lagTimes[7]!=0) return;
		if (flagsCurrent[9]&&flagsCurrent[8]){
			System.out.println("Rotate down");
			owner.subject.setRotate(0,-1,0);
			lagTimes[6] = lagValue;
			lagTimes[7] = (gestureRepeatTime);
		}		
	}
	/**
	 * The gesture defined by both of the user's hands going left.
	 */
	private void handsLeft8() {
		if (lagTimes[8]!=0) return;
		if (flagsCurrent[11]&&flagsCurrent[10]){
			System.out.println("Rotate left");
			owner.subject.setRotate(-1,0,0);
			lagTimes[9] = lagValue;
			lagTimes[8] = (gestureRepeatTime);
		}		
	}
	/**
	 * The gesture defined by both of the user's hands going right.
	 */
	private void handsRight9() {
		if (lagTimes[9]!=0) return;
		if (flagsCurrent[13]&&flagsCurrent[12]){
			System.out.println("Rotate right");
			owner.subject.setRotate(1,0,0);
			lagTimes[8] = lagValue;
			lagTimes[9] = (gestureRepeatTime);
		}		
	}
	
	//Try instead using hands above/behind head, elbows out, for n ms
	/**
	 * The gesture defined by both of the user's hands going forwards quickly.
	 */
	private void handsForward12(){
		if (lagTimes[12]!=0) return;
		if (flagsCurrent[14]&&flagsCurrent[15]){
			System.out.println("Pushed forward - activate/deactivate");
			//Toggle between 1 and 0
			gestureMode = 1-gestureMode;
			//Get rid of any outstanding lag times.
			resetLags();
			lagTimes[12] = 1000f;
		}
	}
	
	
	/**
	 * Returns whether the mean speed, along the line between start and end is greater than or equal to than the bound value.
	 * i.e. between point(x,start) and (x, end). Note this is speed, not velocity.
	 * @param time - time taken in ms
	 * @param start - start point, in mm from an origin line
	 * @param end - end point, in mm from an origin line
	 * @param bound - in m/s == mm/ms , the speed that we want to know if we are greater than or equal to
	 * @return
	 */
	private boolean speedComp(float time, float start, float end, float bound){
		float dist = start-end;
		if (dist<0) {
			dist = -dist;
		}
		return (dist>=bound*time);
	}
	/*
	 * These private boolean methods define various common actions that can be done and combined to make more complex ones
	 */
	
	/** The displacement of end from start, in the X direction, in the most recent update. i.e. end-start (x coordinate)
	 * @param start
	 * @param end
	 * @return
	 */
	private float displacementX(joint start, joint end){
		float startX = newPositions[start.ordinal()].x;
		float endX = newPositions[end.ordinal()].x;		
		return (endX -startX);
	}
	/** The displacement of end from start, in the Y direction, in the most recent update. i.e. end-start (y coordinate)
	 * @param start
	 * @param end
	 * @return
	 */
	private float displacementY(joint start, joint end){
		float startY = newPositions[start.ordinal()].y;
		float endY = newPositions[end.ordinal()].y;		
		return (endY -startY);
	}
	/** The displacement of end from start, in the Z direction, in the most recent update. i.e. end-start (z coordinate)
	 * @param start
	 * @param end
	 * @return
	 */
	private float displacementZ(joint start, joint end){
		float startZ = newPositions[start.ordinal()].z;
		float endZ = newPositions[end.ordinal()].z;		
		return (endZ -startZ);
	}
	/**
	 * Calculates the Euclidean, ||.||2, distance between two points
	 * @param a
	 * @param b
	 * @return
	 */
	private float distBetween(L3DPoint a, L3DPoint b){
		return (float) Math.sqrt((a.x -b.x)*(a.x -b.x)+(a.y -b.y)*(a.y -b.y)+(a.z -b.z)*(a.z -b.z));
	}
	
	/**
	 * Returns whether the user this represents is active.
	 * @return - true if the user this represents is active, false otherwise.
	 */
	public boolean getActive(){
		return gestureMode ==1;
	}
	
}