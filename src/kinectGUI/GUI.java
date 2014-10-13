package kinectGUI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.color.ColorSpace;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;

import javax.swing.JFrame;

import main.ControlManager;
import main.KinectGUI;
import main.KinectHandler;
import main.KinectProjectMain;
import main.L3DPoint;



/**
 * An implementation of the KinectGUI interface, using display methods based on/copied from the OpenNI sample program UserTracker
 * Note, any dimensions given for the size of the output/kinect data are for the case of scale = 1, i.e. 640x480. scale is applied in all dimensions, so 0.5 gives a 320x240, etc. 
 * @author Stephen
 *
 */
public class GUI extends KinectGUI {

	/**
	 * The long serialVersionUID required due to extending KinectGUI.
	 */
	private static final long serialVersionUID = 1L;
	/*	Inherited fields
	 * int screenWidth 
	 * int screenHeight
	 * int kinectWidth 
	 * int kinectHeight
	 * KinectHandler kinectHandler 
	 * ControlManager ctrlManager
	 * float scale
	 */
	/**
	 * Scales the screen size to fit the Kinect image data. i.e. applied to (a point on) the screen, maps it onto the Kinect depth/rgb image.
	 */
	protected float scrToKinScaler;
	/**
	 * Flag for whether the controlling hand should be marked on the output. True means it should be.
	 */
	private boolean plotHand = true;
	/**
	 * Flag for which of RGB or Depth image should be shown. True is RGB, Depth otherwise
	 */
	private boolean showRGB;
	/**
	 * The size of the (minimum) margin around the projection of the screen onto the Kinect data.
	 * A local copy of ControlManager's, copied as accessed repeatedly, and it does not change
	 */
	private int margin;//move to parent??
    /**
     * The JFrame the output will be displayed in. Is a separate variable so it can be closed.
     */
	protected JFrame frame;
	//These fields copied from UserTracker. Descriptions mine.
	/**
	 * The byte array that forms the image to be displayed.
	 */
	private byte[] imgbytes;
	/**
	 * The histogram used for determining the colouration of the depth data
	 */
    private float histogram[];   
    /**
     * Not entirely sure what purpose it serves (looks like not much), but I don't want to break anything.
     */
    private boolean drawBackground = true;
   /**
    * Whether to display the depth/RGB image (and colour in users).
    */
    private boolean drawPixels = true;
    /**
     * Whether users' skeletons should be drawn.
     */
    private boolean drawSkeleton = true;
   /**
    * The actual image, constructed from imgbytes.
    */
    private BufferedImage bimg;
   /**
    * A list of colours.
    */
    Color colours[] = {Color.RED, Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.PINK, Color.YELLOW, Color.WHITE};
    
    /**
	 * A Constructor for the class. Calls GUI(KinectProjectMain main, KinectHandler khand, ControlManager manager, float scale), with scale = 1.
	 * @param main - the KinectProjectMain running the Kinect code, for access to its methods
	 * @param khand - the KinectHandler that holds the data to display
	 * @param manager - the ControlManager that will provide any gesture data that is to be displayed
	 */
    public GUI(KinectProjectMain main, KinectHandler khand, ControlManager manager) {
		this(main, khand, manager, 1f);
	}
	/**
	 * A Constructor for the class. Takes as a parameter the KinectHandler being used, and stores a reference, so it can get data from it. Similarly for the ControlManager being used. Also sets the scale to output image at.
	 * Initialises the fields.
	 * Should split into initialise (setting fields), and setup (creating frame, etc)
	 * @param main - the KinectProjectMain running the Kinect code, for access to its methods
	 * @param khand - the KinectHandler that holds the data to display
	 * @param manager - the ControlManager that will provide any gesture data that is to be displayed
	 * @param scale - the scale at which this is to be shown
	 */
	public GUI(final KinectProjectMain main, KinectHandler khand, ControlManager manager, float scale){
		System.out.println("creating GUI");
		kinectHandler = khand;
		ctrlManager = manager;
		screenWidth = Toolkit .getDefaultToolkit().getScreenSize().width;
    	screenHeight = Toolkit .getDefaultToolkit().getScreenSize().height;
    	kinectWidth = kinectHandler.kinectWidth;
		kinectHeight = kinectHandler.kinectHeight;
		this.scale = scale;
		margin = ctrlManager.margin;
		
		scrToKinScaler = (float) (kinectHeight-(2*margin))/(float)(screenHeight);
	    float temp = (float) (kinectWidth-(2*margin))/(float) (screenWidth);
	    if (scrToKinScaler>temp) scrToKinScaler = temp;
		//These two from UserTracker
	    histogram = new float[10000];        
        imgbytes = new byte[kinectWidth*kinectHeight*3]; 
        
        showRGB = false;
        //Create and set up the frame
        frame = new JFrame("Kinect Window");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
            	//System.exit(0);//want to turn off kinect instead
            	main.turnOffKinect();
            	}
        });
        
        frame.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent arg0) {}
			@Override
			public void keyReleased(KeyEvent arg0) {}
			@Override
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_SPACE)
				{	
					showRGB = !(showRGB);
				}
				if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					System.exit(0);
				}
			}
		});
        frame.add(this);        
        frame.pack();
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
        System.out.println("created GUI");
	}
	/**
	 * Copied from UserTracker, produces a depth image from the input ShortBuffer.
	 * Not used if showing RGB image
	 * @param depth - the ShortBuffer containing the depth data
	 */
	private void calcHist(ShortBuffer depth)
    {
        // reset
        for (int i = 0; i < histogram.length; ++i)
            histogram[i] = 0;        
        depth.rewind();
        int points = 0;
        while(depth.remaining() > 0)
        {
            short depthVal = depth.get();
            if (depthVal != 0)
            {
                histogram[depthVal]++;
                points++;
            }
        }        
        for (int i = 1; i < histogram.length; i++)
        {
            histogram[i] += histogram[i-1];
        }
        if (points > 0)
        {
            for (int i = 1; i < histogram.length; i++)
            {
                histogram[i] = 1.0f - (histogram[i] / (float)points);
            }
        }
    }
	
	@Override
	public void update() { //Main body copied from UserTracker and adapted 
		ShortBuffer scene = kinectHandler.sceneBuffer;
		scene.rewind();
		if (showRGB) {//Colour image version - output is RGB image, with superposition of users coloured in
			ByteBuffer rgb = kinectHandler.imageBuffer;
			rgb.rewind();
			while(scene.remaining() > 0){
				//note that rgb is 3 x size of scene, depth
				int pos = scene.position();
				short user = scene.get();
		    
				imgbytes[3*pos] = rgb.get();
				imgbytes[3*pos+1] = rgb.get();
				imgbytes[3*pos+2] = rgb.get();            
				if (drawBackground &&(user != 0))
				{  	int colourID = user % (colours.length-1);   
					imgbytes[3*pos] = (byte)(colours[colourID].getRed());
					imgbytes[3*pos+1] = (byte)(colours[colourID].getGreen());
					imgbytes[3*pos+2] = (byte)(colours[colourID].getBlue());
				}
			}
		}else{ //Depth data version - Output is depth map, with superposition of user bodies coloured in
		ShortBuffer depth = kinectHandler.depthBuffer;
		depth.rewind();
		calcHist(depth);
		depth.rewind();
		            
		while(depth.remaining() > 0){
			int pos = depth.position();
		    short pixel = depth.get();
		    short user = scene.get();
		                
		    imgbytes[3*pos] = 0;
		    imgbytes[3*pos+1] = 0;
		    imgbytes[3*pos+2] = 0;                	

		    if (drawBackground || pixel != 0)
		    {
		    	int colourID = user % (colours.length-1);
		        if (user == 0)
		        {
		        	colourID = colours.length-1;
		        }
		        if (pixel != 0)
		        {
		        	float histValue = histogram[pixel];
		        	imgbytes[3*pos] = (byte)(histValue*colours[colourID].getRed());
		            imgbytes[3*pos+1] = (byte)(histValue*colours[colourID].getGreen());
		            imgbytes[3*pos+2] = (byte)(histValue*colours[colourID].getBlue());
		        }
		    }
		}
		}
		repaint();		
	}

	//From this point down, copied/abridged from UserTracker, and 'translated' so variables, etc, match.
	public Dimension getPreferredSize() {
        return new Dimension((int) (scale*kinectWidth),(int) (scale*kinectHeight));
    }
	@Override
	public void paint(Graphics g)
    {
    	if (drawPixels)
    	{
            DataBufferByte dataBuffer = new DataBufferByte(imgbytes, kinectWidth*kinectHeight*3);
            WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, kinectWidth, kinectHeight, kinectWidth * 3, 3, new int[]{0, 1, 2}, null); 
            ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8}, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
            bimg = new BufferedImage(colorModel, raster, false, null);
    		g.drawImage(bimg.getScaledInstance((int) (scale*kinectWidth), (int) (scale*kinectHeight), BufferedImage.SCALE_FAST), 0, 0, null);
    		drawScreen(g);//added
    	}
        
			int[] users = kinectHandler.users;
			for (int i = 0; i < users.length; ++i)
			{
		    	Color c = colours[users[i]%colours.length];
		    	c = new Color(255-c.getRed(), 255-c.getGreen(), 255-c.getBlue());
		    	g.setColor(c);
				if (drawSkeleton && kinectHandler.isSkeletonTracking(users[i]))
				{
					drawSkeleton(g, users[i]);
				}
								
			}	
			if (plotHand)
			{
				plotHand(g);
			}
    }
	/**
	 * Draws a line between two joints
	 * @param g - the Graphics to use.
	 * @param jointHash - a HashMap (for the user the joints belong to) mapping joints to their positions in 3D space
	 * @param joint1 - the joint to form one end of the line
	 * @param joint2 - the joint to form the other end of the line
	 */
	void drawLine(Graphics g, HashMap<KinectHandler.joint, L3DPoint> jointHash, KinectHandler.joint joint1, KinectHandler.joint joint2)
    {
		L3DPoint pos1 = jointHash.get(joint1);
		L3DPoint pos2 = jointHash.get(joint2);

		if ((pos1 != null) &&(pos2 != null)) g.drawLine((int) (scale*(pos1.x)), (int) (scale*(pos1.y)), (int) (scale*(pos2.x)), (int) (scale*(pos2.y)));
    }
	/**
	 * Draws a given user's 'Skeleton' composed of lines linking connected joints.
	 * @param g - the Graphics to use
	 * @param user - the user whose Skeleton is to be drawn
	 */
    public void drawSkeleton(Graphics g, int user) 
    {
    	HashMap<KinectHandler.joint, L3DPoint> dict = kinectHandler.skeletonsProjective.get(new Integer(user));
    	
    	g.setColor(Color.RED);
		
    	drawLine(g, dict, KinectHandler.joint.HEAD, KinectHandler.joint.NECK);
    	drawLine(g, dict, KinectHandler.joint.LEFT_SHOULDER, KinectHandler.joint.TORSO);
    	drawLine(g, dict, KinectHandler.joint.RIGHT_SHOULDER, KinectHandler.joint.TORSO);
		drawLine(g, dict, KinectHandler.joint.NECK, KinectHandler.joint.LEFT_SHOULDER);
		drawLine(g, dict, KinectHandler.joint.LEFT_SHOULDER, KinectHandler.joint.LEFT_ELBOW);
    	drawLine(g, dict, KinectHandler.joint.LEFT_ELBOW, KinectHandler.joint.LEFT_HAND);
    	drawLine(g, dict, KinectHandler.joint.NECK, KinectHandler.joint.RIGHT_SHOULDER);
		drawLine(g, dict, KinectHandler.joint.RIGHT_SHOULDER, KinectHandler.joint.RIGHT_ELBOW);
    	drawLine(g, dict, KinectHandler.joint.RIGHT_ELBOW, KinectHandler.joint.RIGHT_HAND);
    	//Below commented out but left in, in case move back to full skeleton
    	/*drawLine(g, dict, KinectHandler.joint.LEFT_HIP, KinectHandler.joint.TORSO);
    	drawLine(g, dict, KinectHandler.joint.RIGHT_HIP, KinectHandler.joint.TORSO);
    	drawLine(g, dict, KinectHandler.joint.LEFT_HIP, KinectHandler.joint.RIGHT_HIP);
    	drawLine(g, dict, KinectHandler.joint.LEFT_HIP, KinectHandler.joint.LEFT_KNEE);
    	drawLine(g, dict, KinectHandler.joint.LEFT_KNEE, KinectHandler.joint.LEFT_FOOT);
    	drawLine(g, dict, KinectHandler.joint.RIGHT_HIP, KinectHandler.joint.RIGHT_KNEE);
		drawLine(g, dict, KinectHandler.joint.RIGHT_KNEE, KinectHandler.joint.RIGHT_FOOT);*/
    	
    }   

    //These two are mine, but heavily 'inspired' by the original drawSkeleton
    /**
     * Draws a rectangle to represent the screen onto the depth image
     * @param g - the Graphics to use
     */
	private void drawScreen(Graphics g) {
		Color oldColor = g.getColor();
		g.setColor(Color.GREEN);
		g.drawRect((int) (scale*margin), (int) (scale*margin), (int) (scale*screenWidth*scrToKinScaler),(int) (scale*screenHeight*scrToKinScaler));
		g.setColor(oldColor);		
	}

	/**
	 * Marks out the controlling hand - the one moving the mouse.
	 * Separate from rest of Skeleton in case only want one or other
	 * @param g - the Graphics to use
	 * @param user - the user whose hand we want
	 */
	private void plotHand(Graphics g)  {
		L3DPoint pos = ctrlManager.controlHandPos;
		if ((pos != null))
		{
			Color oldColour = g.getColor();
	    	g.setColor(Color.WHITE);
			g.fillOval((int) (scale*(pos.x-5)), (int) (scale*(pos.y-5)),(int) (scale*10),(int) (scale*10));
			g.setColor(oldColour);
		}		
	}
	@Override
	public void closeWindow() {
		frame.dispose();		
	}

}
