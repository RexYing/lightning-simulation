package dbm;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;

import com.jogamp.opengl.GL2;

/**
 * Interaction central: Handles windowing/mouse events, and building state.
 */
class GUI implements MouseListener, MouseMotionListener, KeyListener {
	boolean simulate = false;

	/** Current build task (or null) */
	Task task;

	JFrame guiFrame;
	TaskSelector taskSelector = new TaskSelector();
	
	AdaptiveGrid simulation;

	GUI() {
		guiFrame = new JFrame("Tasks");
		guiFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		guiFrame.setLayout(new SpringLayout());
		guiFrame.setLayout(new GridLayout(6, 1));

		/* Add new task buttons here, then add their functionality below. */
		ButtonGroup buttonGroup = new ButtonGroup();
		AbstractButton[] buttons = { new JButton("Reset"), new JButton("Load File"), new JButton("Load OBJ"),
				new JToggleButton("Exit", false), new JToggleButton("[Some Other Task]", false), };

		for (int i = 0; i < buttons.length; i++) {
			buttonGroup.add(buttons[i]);
			guiFrame.add(buttons[i]);
			buttons[i].addActionListener(taskSelector);
		}

		guiFrame.setSize(200, 200);
		guiFrame.pack();
		guiFrame.setVisible(true);

		// task = null; // Set default task here
		task = null;
	}
	
	void addSimulation(AdaptiveGrid simulation) {
		this.simulation = simulation;
	}

	/**
	 * Simulate then display particle system and any builder adornments.
	 */
	void simulateAndDisplayScene(GL2 gl) {
		/*
		if (simulate) {
			PS.advanceTime(DT);
		}

		// Draw particles, forces, etc.
		PS.display(gl);

		if (simulate && frameExporter != null) {
			frameExporter.writeFrame(gl);
		}
		*/
		simulation.display(gl);
		boolean particleAdded = simulation.addParticle();
		
		// Display task if any
		if (task != null)
			task.display(gl);
		
	}

	/**
	 * ActionListener implementation to manage Task selection using (radio)
	 * buttons.
	 */
	class TaskSelector implements ActionListener {

		/**
		 * Resets ParticleSystem to undeformed/material state, disables the
		 * simulation, and removes the active Task.
		 */
		void resetToRest() {
			//PS.reset();// synchronized
			simulate = false;
			task = null;
		}

		/**
		 * Creates new Task objects to handle specified button action. Switch to
		 * a new task, or perform custom button actions here.
		 */
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			System.out.println(cmd);

			if (cmd.equals("Reset")) {
				if (task != null) {
					task.reset();
				} else {
					resetToRest(); // set task=null
				}
			} else if (cmd.equals("Exit")) {
				task = new PlaceholderTask();
			} else if (cmd.equals("Load File")) {
				System.out.println("Load from file");
				//loadFrameFromFile();
			} else {
				System.out.println("UNHANDLED ActionEvent: " + e);
			}
		}
	}

	// Methods required for the implementation of MouseListener
	public void mouseEntered(MouseEvent e) {
		if (task != null)
			task.mouseEntered(e);
	}

	public void mouseExited(MouseEvent e) {
		if (task != null)
			task.mouseExited(e);
	}

	public void mousePressed(MouseEvent e) {
		if (task != null)
			task.mousePressed(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (task != null)
			task.mouseReleased(e);
	}

	public void mouseClicked(MouseEvent e) {
		if (task != null)
			task.mouseClicked(e);
	}

	// Methods required for the implementation of MouseMotionListener
	public void mouseDragged(MouseEvent e) {
		if (task != null)
			task.mouseDragged(e);
	}

	public void mouseMoved(MouseEvent e) {
		if (task != null)
			task.mouseMoved(e);
	}

	// Methods required for the implementation of KeyListener
	public void keyTyped(KeyEvent e) {
	} // NOP

	public void keyPressed(KeyEvent e) {
		dispatchKey(e);
	}

	public void keyReleased(KeyEvent e) {
	} // NOP

	/**
	 * Handles keyboard events, e.g., spacebar toggles simulation/pausing, and
	 * escape resets the current Task.
	 */
	public void dispatchKey(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			simulate = !simulate;
			if (simulate) {
				System.out.println("Starting simulation...");
			} else {
				System.out.println("Simulation paused.");
			}
			break;
		case KeyEvent.VK_ESCAPE:
			taskSelector.resetToRest(); // sets task=null;
			break;
		case KeyEvent.VK_E:
			break;
		case KeyEvent.VK_I:
			break;
		case KeyEvent.VK_L:
			break;
		case KeyEvent.VK_EQUALS:
			break;
		case KeyEvent.VK_MINUS:
			break;
		case KeyEvent.VK_LEFT:
			break;
		case KeyEvent.VK_RIGHT:
			break;
		case KeyEvent.VK_UP:
			break;
		case KeyEvent.VK_DOWN:
			break;

		default:
		}
	}

	/**
	 * "Task" command base-class extended to support building/interaction via
	 * mouse interface. All objects extending Task are implemented here as inner
	 * classes for simplicity.
	 *
	 * Add tasks as necessary for different interaction modes.
	 */
	abstract class Task implements MouseListener, MouseMotionListener {
		/**
		 * Displays any task-specific OpengGL information, e.g., highlights,
		 * etc.
		 */
		public void display(GL2 gl) {
		}

		// Methods required for the implementation of MouseListener
		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
		}

		// Methods required for the implementation of MouseMotionListener
		public void mouseDragged(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
		}

		/**
		 * Override to specify reset behavior during "escape" button events,
		 * etc.
		 */
		abstract void reset();

	}

	/** dummy task that does nothing. */
	class PlaceholderTask extends Task {
		public void mousePressed(MouseEvent e) {
		}

		void reset() {
			taskSelector.resetToRest(); // sets task=null;
		}
	}


}
