package dbm;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL2;

/**
 * 
 * Implement the adaptive quadtree data structure for dielectric breakdown model.
 * @author rex
 *
 */
public class AdaptiveGrid {
	/** True if simulation has reached the termination point */
	private boolean terminated;
	
	private BalancedQuadtree quadtree;
	
	private int totalNumParticles = 0;
	private int numNewParticlesBeforeSolve = SimulationConstants.SKIP;
	
	private List<QuadtreeNode> candidates = new ArrayList<>();
	
	public AdaptiveGrid(int gridWidth, int gridHeight, Point2D start, Point2D termination) {
		this.quadtree = new BalancedQuadtree(gridWidth, gridHeight);
		
		QuadtreeNode startNode = quadtree.setStart(start.getX(), start.getY());
		candidates.addAll(quadtree.checkCandidate(startNode));
		quadtree.setTermination(termination.getX(), termination.getY());

	}
	
	public void display(GL2 gl) {
		gl.glPushMatrix();
		gl.glTranslated(-0.5, -0.5, 0);

		for (QuadtreeNode node : quadtree.getLeaves()) {
			if (node.isBoundary)
				quadtree.drawNode(node, 0, 0, Math.max(node.potential, 0), gl);
			else
				quadtree.drawNode(node, Math.max(node.potential, 0), 0, 0, gl);
		}
		quadtree.drawBoundary(gl);

		gl.glPopMatrix();
		
	}
	
	/**
	 * 
	 * @return true if particle is added in simulation.
	 */
	public boolean addParticle() {
		int iter = 0;
		if (numNewParticlesBeforeSolve == SimulationConstants.SKIP) {
			numNewParticlesBeforeSolve = 0;
			iter = quadtree.solve();
		} else {
			numNewParticlesBeforeSolve++;
		}
		
		
		
		return true;
	}
	
	

}
