package subjectManager;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JToolBar;

import main.KinectSubject;

import com.jogamp.opengl.util.Animator;
/* rotation angle a about vector x,y,z; s=sin a, c=cos a, x,y,z scaled so unit vector
 * x^2*(1 - c) + c		x*y*(1 - c) - z*s 	x*z*(1 - c)+ y*s 	0
	y*x*(1 - c) + z*s 	y^2*(1 - c)+ c 		y*z*(1 - c) - x*s 	0
	x*z*(1 - c) - y*s 	y*z*(1 - c) + x*s 	z^2*(1 - c) + c	 	0 
	0					0					0					1
	Thanks to wikipedia and gl documentation
 */
/**
 * My stand in for CyberVis, a highly exciting multicoloured cube rendered in JOGL.
 * @author Stephen
 *
 */
public class DemoSubject implements GLEventListener, KinectSubject{
	
	/**
	 * The Frame containing the canvas.
	 */
	private Frame frame;
	/**
	 * The rotation increment angle. Is in degrees.
	 */
	private double rotateIncrement =22.5; //degrees
	/**
	 * Variable used it varying appearance of cube, to show that being updated. Treated as an angle in degrees.
	 */
	private double delta =0; 
	/**
	 * The sine of delta.
	 */
	private double s = 1;
	
	/**
	 * Scale factor for zooming in.
	 */
	private double sfp = 1.1;
	/**
	 * Scale factor for zooming out, is inverse of zoom in one
	 */
	private double sfm = 1/sfp;
	/**
	 * The standard increment for shifting the cube in any direction
	 */
	private double shiftIncrement = 0.25;
	
	//Flags for controlling view
	/**
	 * Flag denoting whether the view needs to be reset
	 */
	public boolean resetFlag = false;
	/**
	 * Flag indicating any zooming to be done. Positive is zoom in that number of times, negative, zoom out.
	 */
	private int zoomFlag = 0;
	/**
	 * How much we wish to shift along the X axis
	 */
	private double shiftX =0;
	/**
	 * How much we wish to shift along the Y axis
	 */
	private double shiftY =0;
	/**
	 * How much we wish to shift along the Z axis
	 */
	private double shiftZ =0;
	/**
	 * How much we wish to rotate around the X axis
	 */
	private double rotateX =0;
	/**
	 * How much we wish to rotate around the Y axis
	 */
	private double rotateY =0;
	/**
	 * How much we wish to rotate around the Z axis
	 */
	private double rotateZ =0;
	
	/**
	 * Constructor. Sets up the Canvas, sets up the Frame to hold it, then sets up and starts the Animator (for/on the Canvas).
	 */
	public DemoSubject(){
		GLProfile glp = GLProfile.getDefault(); 
        GLCapabilities caps = new GLCapabilities(glp); 
        GLCanvas canvas = new GLCanvas(caps); 
        setupFrame(canvas);
        canvas.addGLEventListener(this);
        Animator animator = new Animator(canvas);
        animator.add(canvas);
        animator.start();
	}
	/**
	 * Sets up the Frame to contain the canvas. Also adds listeners to it for command inputs.
	 * @param canvas - the Canvas to be contained by the new Frame.
	 */
	private void setupFrame(GLCanvas canvas){
		frame = new Frame("Cybervis Stand-in");
        frame.setSize(1000, 1000);
        frame.add(canvas);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        //As soon as click elsewhere, this no longer works - WHY??
        frame.addKeyListener(new KeyListener(){

			@Override
			public void keyPressed(KeyEvent arg0) {
				//Commands: reset, move up/down/left/right, rotate l/r/u/p zoom +/-
				 switch(arg0.getKeyCode()){
					case( KeyEvent.VK_ESCAPE):{
					 	System.exit(0);
						break;}
					case( KeyEvent.VK_BACK_SPACE):{
					 	//reset projection
						resetFlag = true;
						break;}
					case( KeyEvent.VK_EQUALS):{
					 	//zoom in
						setZoom(1);
						break;}
					case( KeyEvent.VK_MINUS):{
					 	//zoom out
						setZoom(-1);
						break;}
					case( KeyEvent.VK_UP):{
					 	//move view up
						setShift(0,1,0);
						System.out.println("move up");
						break;}
					case( KeyEvent.VK_DOWN):{
					 	//move view down
						setShift(0,-1,0);
						System.out.println("move down");
						break;}
					case( KeyEvent.VK_LEFT):{
					 	//move view left
						setShift(-1,0,0);
						System.out.println("move left");
						break;}
					case( KeyEvent.VK_RIGHT):{
					 	//move view right
						setShift(1,0,0);
						System.out.println("move right");
						break;}
					case( KeyEvent.VK_NUMPAD8):{
					 	//rotate view up
						setRotate(0,1,0);
						System.out.println("rotate up");
						break;}
					case( KeyEvent.VK_NUMPAD2):{
					 	//rotate view down
						setRotate(0,-1,0);
						System.out.println("rotate down");
						break;}
					case( KeyEvent.VK_NUMPAD4):{
					 	//rotate view left
						setRotate(-1,0,0);
						System.out.println("rotate left");
						break;}
					case( KeyEvent.VK_NUMPAD6):{
					 	//rotate view right
						setRotate(1,0,0);
						System.out.println("rotate right");
						break;}
					}				 
			}
			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub				
			}
			@Override
			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub				
			}        	
        });
	}
	/*
	 * Methods to control changing of view
	 */
	public void setZoom(int zoomChange){
		zoomFlag =zoomFlag+zoomChange;
	}
	public void setShift(int xShift, int yShift, int zShift){
		shiftX = shiftX+xShift*shiftIncrement;
		shiftY = shiftY+yShift*shiftIncrement;
		shiftZ = shiftZ+zShift*shiftIncrement;		
	}
	public void setRotate(int xRotate, int yRotate, int zRotate){
		rotateX = rotateX+xRotate*rotateIncrement;
		rotateY = rotateY+yRotate*rotateIncrement;
		rotateZ = rotateZ+zRotate*rotateIncrement;		
	}
	/**
	 * If zoomFlag >0 it zooms in, if zoomFlag is  <0 it zooms out. Magnitude of zoomFlag indicates how many times.
	 * Assumes that it is already in Projection matrix mode.
	 * @param gl - the GL2 being used
	 */
	protected void zoomView(GL2 gl) {// Note we assume we are already in Projection matrix
		int local = zoomFlag;
		while(local>0) {
			gl.glScaled(sfp, sfp,sfp);
			System.out.println("zoom in");
			local = local-1;
			}
		while (local<0) {
			gl.glScaled(sfm, sfm,sfm);
			System.out.println("zoom out");
			local = local+1;
		}
		zoomFlag =0;
	}

	/**
	 * Resets the view to the specified start position. Assumes that are already in Projection matrix mode.
	 * @param gl - the GL2 being used
	 */
	protected void resetView(GL2 gl) {// Note assume are already in Projection matrix
		gl.glLoadIdentity();  // Reset The Projection Matrix
	    gl.glScaled(0.5, 0.5,0.5);//zoom out a bit
	    gl.glTranslated(1, 0, 0);//offset horizontally
	    //adjust position slightly, setting other flags to default values.
	    rotateX = -rotateIncrement;
		rotateY = -rotateIncrement;
		rotateZ =0;
		shiftX = 0;
		shiftY =0;
		shiftZ =0;
		zoomFlag =0;
		resetFlag = false;
	    System.out.println("reset");		
	}
	/**
	 * Shifts the subject being viewed based on the shift flag values.  Assumes that are already in Projection matrix mode.
	 * Have replaced use of translate with incrementing matrix positions directly, as want movement 
	 * to be with respect to the user's Point of view.
	 * @param gl - the GL2 being used
	 */
	protected void shiftView(GL2 gl) {// Note assumes we are already in Projection matrix
		double[] projMatrix = new double[16];
		gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projMatrix, 0);
		projMatrix[12] = projMatrix[12]+ shiftX;
		projMatrix[13] = projMatrix[13]+ shiftY;
		projMatrix[14] = projMatrix[14]+ shiftZ;
		gl.glLoadMatrixd(projMatrix, 0);
		System.out.println("Shifted: right "+ shiftX+", up "+shiftY+", out "+shiftZ);
		shiftX = 0;
		shiftY =0;
		shiftZ =0;
	}

	/**
	 * Prints out matrix - used to check alterations to shiftView, rotateView
	 * Check matrix form. think gl is in column major
	 * @param projMatrix - the matrix I want outputted
	 */
	private void printout(double[] projMatrix) {
		for (int i =0; i<4; i=i+1){
			for (int j =0; j<4; j=j+1){
				System.out.print(projMatrix[4*j+i]+" ");
			}
			System.out.println();
		}
		
	}

	/**
	 * Rotates the subject being viewed based on the rotate flag values.  Assumes that we are already in Projection matrix mode.
	 * @param gl - the GL2 being used
	 */
	protected void rotateView(GL2 gl) {// Note assume are already in Projection matrix
		double[] projMatrix = new double[16];
		gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projMatrix, 0);
		gl.glRotated(rotateY,projMatrix[0],projMatrix[4], projMatrix[8]);	 //(angle,x,y,z) xyz define axis
		gl.glRotated(-rotateX,projMatrix[1],projMatrix[5], projMatrix[9]);	 //(angle,x,y,z) xyz define axis
		gl.glRotated(rotateZ,projMatrix[2],projMatrix[6], projMatrix[10]);	 //(angle,x,y,z) xyz define axis
		System.out.println("Rotated: right "+ rotateX+", up "+rotateY+", out "+rotateZ +" degrees");
		rotateX = 0;
		rotateY =0;
		rotateZ =0;
	}
	@Override
	public void display(GLAutoDrawable drawable) {
		update(drawable);
	    draw(drawable);
	}

	/**
	 * Draws the current model to match the current state.
	 * @param drawable
	 */
	private void draw(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();	   
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);   // Select The Modelview Matrix
	    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);     // Clear The Screen And The Depth Buffer
	    gl.glLoadIdentity();   // Reset The View
	    //change the strength of the colouring
	    s = Math.sin(delta);
	    double strength = s/4+0.75;
	    //Start drawing the cube
	    gl.glBegin(GL2.GL_QUADS);
	    
	  //Top
	    gl.glColor3d(strength, 0, 0);
	    gl.glVertex3f(-0.5f, 0.5f, -0.5f);              // Top/back Left
	    gl.glVertex3f( 0.5f, 0.5f, -0.5f);              // Top/back Right
	    gl.glVertex3f( 0.5f, 0.5f, 0.5f);              // Bottom/near Right
	    gl.glVertex3f(-0.5f, 0.5f, 0.5f);              // Bottom/near Left
	    
	  //Front
	    gl.glColor3d(0, strength, 0);
	    gl.glVertex3f(-0.5f, 0.5f, 0.5f);              // Top Left
	    gl.glVertex3f( 0.5f, 0.5f, 0.5f);              // Top Right
	    gl.glVertex3f( 0.5f,-0.5f, 0.5f);              // Bottom Right
	    gl.glVertex3f(-0.5f,-0.5f, 0.5f);              // Bottom Left
	    
	  //Right
	    gl.glColor3d(0, 0, strength);
	    gl.glVertex3f( 0.5f, 0.5f, 0.5f);              // Top Left/near
	    gl.glVertex3f( 0.5f, 0.5f,-0.5f);              // Top Right/far
	    gl.glVertex3f( 0.5f,-0.5f,-0.5f);              // Bottom Right/far
	    gl.glVertex3f( 0.5f,-0.5f, 0.5f);              // Bottom Left/near
	    
	  //Back - Left is wrt looking at face from that side, left is  from default user position
	    gl.glColor3d(strength,0, strength);
	    gl.glVertex3f( 0.5f, 0.5f,-0.5f);              // Top Left/right
	    gl.glVertex3f( -0.5f, 0.5f,-0.5f);              // Top Right/left
	    gl.glVertex3f( -0.5f,-0.5f,-0.5f);              // Bottom Right/left
	    gl.glVertex3f( 0.5f,-0.5f,-0.5f);              // Bottom Left/right
	    
	  //Left
	    gl.glColor3d(strength, strength,0);
	    gl.glVertex3f(-0.5f, 0.5f,-0.5f);              // Top Left/far
	    gl.glVertex3f(-0.5f, 0.5f,0.5f);              // Top Right/near
	    gl.glVertex3f(-0.5f,-0.5f,0.5f);              // Bottom Right/near
	    gl.glVertex3f(-0.5f,-0.5f,-0.5f);              // Bottom Left/far
	  
	    //Bottom - Left... as if rotated to be front, & front ->top
	    gl.glColor3d(0, strength, strength);
	    gl.glVertex3f(-0.5f, -0.5f,  0.5f);              // Top/near Left
	    gl.glVertex3f( 0.5f, -0.5f,  0.5f);              // Top/near Right
	    gl.glVertex3f( 0.5f,  -0.5f, -0.5f);              // Bottom/far Right
	    gl.glVertex3f(-0.5f, -0.5f, -0.5f);              // Bottom/far Left
	    
	    gl.glEnd();
		
	}

	/**
	 * Gets the current gl, enters Projection matrix mode, and goes through the control flags calling the appropriate methods in each case.
	 * Once they are done, updates the system's state variables
	 * @param drawable
	 */
	public void update(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
	    gl.glMatrixMode(GL2.GL_PROJECTION);    // Select The Projection Matrix
	    if (resetFlag) resetView(gl);//If the system wants to be reset, do so
	    if (zoomFlag !=0) zoomView(gl);//if the system wants to change the zoom do so
	    if ((shiftX !=0)||(shiftY !=0)||(shiftZ !=0))shiftView(gl);//if the system wants to move in  direction, do so
	    if ((rotateX !=0)||(rotateY !=0)||(rotateZ !=0))rotateView(gl);//if the system wants to rotate, do so
	    //Do something - change the variable that determines the colour strength of the cube
	   delta = delta + 0.05;
	       
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		drawable.getGL().setSwapInterval(1);//not sure what does, may remove
		GL2 gl = drawable.getGL().getGL2();
			gl.glClearDepth(1.0f);                      // Depth Buffer Setup
			gl.glEnable(GL.GL_DEPTH_TEST);			// Enables Depth Testing
			gl.glDepthFunc(GL.GL_LEQUAL);			// The Type Of Depth Testing To Do
			gl.glMatrixMode(GL2.GL_PROJECTION);    // Select The Projection Matrix
			resetView(gl);
		System.out.println("Init called");
	}

	@Override
	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
			int arg4) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Creates a stand alone instance of this class.
	 * @param args
	 */
	  public static void main(String[] args){
		new DemoSubject();
	}
	public void addKinectToolBar(JToolBar kinectToolBar) {
		frame.add(BorderLayout.NORTH, kinectToolBar);
		frame.pack();//Check Java 7 Documentation - does add now automatically pack a frame??//seems to be needed for jre6, but not 7		
	}
}
