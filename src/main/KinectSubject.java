package main;

import javax.swing.JToolBar;

/**
 * Generic interface for the Object receiving the gestures deduced from the Kinect output.
 * @author Stephen
 *
 */
public interface KinectSubject {

	//Might want to add reset option
	
	/**
	 * Adds the Kinect control ToolBar to the Object's main frame.
	 * @param kinectToolBar - the JToolBar to add.
	 */
	void addKinectToolBar(JToolBar makeToolBar);

	/**
	 * Increment/decrement the amount of zooming in to be done by the given amount.
	 * @param zoomChange - how much to change the amount of zooming to be done by.
	 */
	void setZoom(int zoomChange);

	/**
	 * Adjust the translation to be done by the given values
	 * @param xShift - how much further to go along X axis
	 * @param yShift - how much further to go along Y axis
	 * @param zShift - how much further to go along Z axis
	 */
	void setShift(int xShift, int yShift, int zShift);

	/**
	 * Adjust the rotation to be done by the given values
	 * @param xRotate - how much further to go around X axis
	 * @param yRotate - how much further to go around Y axis
	 * @param zRotate - how much further to go around Z axis
	 */
	void setRotate(int xRotate, int yRotate, int zRotate);

}
