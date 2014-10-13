package subjectManager;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import main.KinectProjectMain;
import main.KinectSubject;
import main.SubjectManager;
/**
 * Handles the code's interaction with the subject, and provides the toolBar used to turn the Kinect on/off, and display its status.
 * @author Stephen
 *
 */
public class ToolBarSM extends SubjectManager {
	/*
	 * Inherited fields
	 * KinectProjectMain main
	 * KinectSubject subject
	 */
	
	/**
	 * The toolBar that shows the Kinect's status, and allows it to be turned on and off
	 */
	protected JToolBar toolBar;
	/**
	 * The JToggleButton that shows whether the Kinect is pressing the left mouse button
	 */
	private JToggleButton leftPressedButton;
	/**
	 * The JToggleButton that is used to turn the Kinect (accessing/using code) on/off, and which shows which state it is in
	 */
	private JToggleButton onOffButton;
	/**
	 * The list of Users that were recognised the last time displayActive was called - kept for comparison with the next set of users.
	 */
	protected int[] prevUsers;
	/**
	 * An array of JToggleButtons that contains a button for each currently recognised user. The buttons' states indicate whether each user is active or not.
	 */
	protected JToggleButton[] buttons;
	/**
	 * The Insets used for all the JToggleButtons - I wanted the toolBar to have minimum profile, and as this would be needed each time a button was made, thought I'd name it rather than have a new one each time.
	 */
	protected Insets buttonInsets;
	/**
	 * Constructor for the class. Sets up the toolBar, and adds it to the subject's frame.
	 * @param main - The KinectProjectMain running this instance of the Kinect code
	 * @param subject - the KinectSubject the Kinect input will be applied to
	 */
	public ToolBarSM(KinectProjectMain main, KinectSubject subject) {
		super(main, subject);
		buttonInsets = new Insets(0,5,0,5);
		buttons = new JToggleButton[0];
		subject.addKinectToolBar(makeToolBar());
	}

	/**
	 * A method that creates the JToolBar, and assigns the buttons - these will act as a visual reminder of what the current Kinect State is
	 * @return - the JToolBar for the application
	 */
	protected JToolBar makeToolBar(){
		toolBar = new JToolBar();
		toolBar.setMargin(new Insets(0,10,0,10));		
		onOffButton = new JToggleButton("Kinect on");
		onOffButton.setMargin(buttonInsets);
		onOffButton.setSelected(false);
		onOffButton.addActionListener(new ActionListener(){				
			@Override
			public void actionPerformed(ActionEvent e) {
				onOffToggled();
			}			
		});		
		leftPressedButton = new JToggleButton("Left mouse button Pressed (by kinect)");
		leftPressedButton.setMargin(buttonInsets);
		leftPressedButton.setEnabled(false);		
		//Add the buttons to toolBar and return it.
		toolBar.add(onOffButton);
		toolBar.add(leftPressedButton);				
		return toolBar;
	}
	/**
	 * The method to deal with the onOffButton in the toolBar being pressed.
	 * If it was off, it tries to turn the Kinect on, and if that fails triggers an error pop up before toggling it off. If it was on, it turns the Kinect off. Both are done by calling the relevant methods in the KinectProjectMain main.
	 */
	protected void onOffToggled() {
		if (onOffButton.isSelected()) {
			System.out.println("On");
			try{
			main.turnOnKinect();
			}
			catch(Exception e){
				onOffButton.setSelected(false);
				kinectProblem(e.getMessage());
			}			
		}else{
			System.out.println("Off");
			main.turnOffKinect();
		}
		toolBar.repaint();//seems to be needed for jre6, but not 7		
	}
	@Override
	public void refeshMouseStatus(boolean[] mouseStatus){
		if(leftPressedButton.isSelected() != mouseStatus[0] ){//i.e. the current mouse state is different to that being shown
			leftPressedButton.setSelected(mouseStatus[0]);
			toolBar.repaint();//seems to be needed for jre6, but not 7
			}
		}
	@Override
	public void displayActive(int[] users, boolean[] active){
		int len = users.length;
		if(len != active.length) throw new Error("In displayActive, arrays must be of equal length");
		boolean diff = (prevUsers ==null);
		if (!diff) diff = (len != prevUsers.length);
		int j =0;
		while ((j<len)&&!diff){
			diff = (users[j] != prevUsers[j]);
			j=j+1;
		}
		//i.e. at this point diff is true iff the two arrays are different
		if (diff){
			//change the list of buttons
			System.out.println("updating user list");
			int len2 = buttons.length;
			//remove the old buttons from the toolBar
			for(int i = 0; i<len2; i=i+1){
				toolBar.remove(buttons[i]);	
			}
			buttons = new JToggleButton[len];
			for(int i = 0; i<len; i=i+1){
				buttons[i] = new JToggleButton("User"+users[i]);
				buttons[i].setMargin(buttonInsets);
				toolBar.add(buttons[i]);
				buttons[i].setEnabled(false);
			}
			toolBar.doLayout();//seems to be needed for jre6, but not 7
			prevUsers = users;
		}
		//it is implicit that users, buttons are same length
		for(int i = 0; i<len; i=i+1){
			//toggle the button states.
			if(buttons[i].isSelected() != active[i] ){//to prevent flicker after repaint called
				buttons[i].setSelected(active[i]);
				diff = true;//to prevent flicker after repaint called
				}
		}
		if (diff) toolBar.repaint();//i.e. if the toolBar has been changed, repaint it, otherwise don't.		
	}
	@Override
	public void setOnOffStatus(boolean bool){
		onOffButton.setSelected(bool);
	}

	@Override
	public void kinectProblem(String s) {
		//This should have the message come from the frame where the toolBar is
		JOptionPane.showMessageDialog(toolBar, "Error in Kinect code: "+s);
	}	
	
}
