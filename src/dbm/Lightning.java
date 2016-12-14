package dbm;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.Animator;


public class Lightning implements GLEventListener {
	
	private GUI gui;
	private JFrame frame;
	private GLU glu;
	
	private double[] camera = new double[2];
	private double[] translate = new double[2];
	
	private int width;
	private int height;
	
	private AdaptiveGrid lightningSimulation;
	
	public Lightning() {
		width = GUIConstants.DEFAULT_WIDTH;
		height = GUIConstants.DEFAULT_HEIGHT;
		camera[0] = GUIConstants.CAMERA_X;
		camera[1] = GUIConstants.CAMERA_Y;
		translate[0] = GUIConstants.TRANSLATE_X;
		translate[1] = GUIConstants.TRANSLATE_Y;
		
	}

	@Override
	public void display(GLAutoDrawable drawable) {
	    GL2 gl = drawable.getGL().getGL2();
	    
	    
	    gl.glMatrixMode(GL2.GL_PROJECTION);
	    gl.glLoadIdentity();
	    if (glu == null)
	        glu = GLU.createGLU();
	    glu.gluOrtho2D(-camera[0] - translate[0], camera[0] + translate[0], -camera[1] - translate[1], camera[1] + translate[1]);
	    
	    gl.glMatrixMode(GL2.GL_MODELVIEW);
	    gl.glLoadIdentity();
	    
	    //gl.glClearColor(0.5f, 0.5f, 0.5f, 1f);
	    gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
	    gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
	    
	    gui.simulateAndDisplayScene(gl);
	    
	    /*try {
	    	Thread.sleep(100);
	    } catch (InterruptedException e) {
	    	System.out.println("Interrupted");
	    }*/
	    
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
	    System.err.println("INIT GL IS: " + gl.getClass().getName());

	    gl.setSwapInterval(1);
	    //gl.glEnable(GL2.GL_DEPTH_TEST);
	    gl.glLineWidth(1);

	    gl.glEnable(GL2.GL_NORMALIZE);

	    // SETUP LIGHTING
	    float[] lightAmbient = { 0f, 0f, 0f, 1f };
	    float[] lightDiffuse = { 0.9f, 0.9f, 0.9f, 1f };
	    float[] lightSpecular = { 1f, 1f, 1f, 1f };
	    float[] lightPos = { 0f, 0f, 0f, 1f };

	    gl.glMatrixMode(GL2.GL_MODELVIEW);
	    gl.glLoadIdentity();
	    gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
	    gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, lightAmbient, 0);
	    gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDiffuse, 0);
	    gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightSpecular, 0);
	    gl.glEnable(GL2.GL_LIGHT0);
	    

	    /* By default, no attractors are added */
	    List<Point2D> attractors = new ArrayList<>();
		lightningSimulation = new AdaptiveGrid(
				SimulationConstants.WIDTH, SimulationConstants.HEIGHT, new Point2D.Double(0.5, 0.9), new Point2D.Double(0.5, 0.1), attractors);
		//lightningSimulation = new AdaptiveGrid(
		//		SimulationConstants.WIDTH, SimulationConstants.HEIGHT, new Point2D.Double(0.5, 0.9), new Point2D.Double(0.35, 0.62), attractors);
		gui.addSimulation(lightningSimulation);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		System.out.println("width=" + width + ", height=" + height);
	    height = Math.max(height, 1); // avoid height=0;

	    this.width = width;
	    this.height = height;

	    GL2 gl = drawable.getGL().getGL2();
	    gl.glViewport(0, 0, width, height);
	    
	    
	    gl.glMatrixMode(GL2.GL_PROJECTION);
	    gl.glLoadIdentity();
	    if (glu == null)
	        glu = GLU.createGLU();
	    glu.gluOrtho2D(-camera[0] - translate[0], camera[0] + translate[0], -camera[1] - translate[1], camera[1] + translate[1]);
	    
	    gl.glMatrixMode(GL2.GL_MODELVIEW);
	    gl.glLoadIdentity();
	}

	/**
	 * Builds and shows windows/GUI, and starts simulator.
	 */
	public void start() {
		if (frame != null)
			return;

		gui = new GUI();

		frame = new JFrame("Lightning Animator");
		GLProfile glp = GLProfile.getDefault();
		GLCapabilities glc = new GLCapabilities(glp);
		GLCanvas canvas = new GLCanvas(glc);
		canvas.addGLEventListener(this);
		frame.add(canvas);

		canvas.addMouseListener(gui);
		canvas.addMouseMotionListener(gui);
		canvas.addKeyListener(gui);

		final Animator animator = new Animator(canvas);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// Run this on another thread than the AWT event queue to
				// make sure the call to Animator.stop() completes before
				// exiting
				new Thread(new Runnable() {
					public void run() {
						animator.stop();
						System.exit(0);
					}
				}).start();
			}
		});

		frame.pack();
		frame.setSize(width, height);
		frame.setLocation(400, 400);
		frame.setVisible(true);
		animator.start();
	}

	public static void main(String[] args) {
		Lightning lightning = new Lightning();
		lightning.start();

	}
}
