package main;

/**
 * A class to represent a point with float values in 3 dimensions. Exists as a generic 3D point for the purpose of passing location data between packages.
 * Each value to be directly accessed.
 * @author Stephen
 *
 */
public class L3DPoint {
	/**
	 * The float x value.
	 */
	public float x;
	/**
	 * The float y value.
	 */
	public float y;
	/**
	 * The float z value.
	 */
	public float z;
	/**
	 * Construct a new L3DPoint with the given values
	 * @param x - the float x value
	 * @param y - the float y value
	 * @param z - the float z value
	 */
	public L3DPoint(float x, float y, float z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	/**
	 * Construct a new L3DPoint, with no specified values (all set to 0).
	 */
	public L3DPoint(){
		x=0;
		y=0;
		z=0;
	}
	/**
	 * Tests if another L3DPoints is equal to this one in that they have the same parameters
	 * @param other - the L3DPoint this is being compared to.
	 * @return - true if the x values match, the y values match, and the z values match. false otherwise.
	 */
	public boolean equals(L3DPoint other){
		if (other == null) return false;
		return ((other.x ==this.x)&&(other.y ==this.y)&&(other.z == this.z));		
	}
	
}
