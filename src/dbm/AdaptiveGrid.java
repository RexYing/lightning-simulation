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
	private static final double EPS = 1e-8;
	
	/** True if simulation has reached the termination point */
	private boolean terminated;
	
	private BalancedQuadtree quadtree;
	
	private int totalNumParticles = 0;
	private int numNewParticlesBeforeSolve = SimulationConstants.SKIP;
	
	private List<QuadtreeNode> candidates = new ArrayList<>();
	
	private LightningTree lightningTree;
	
	public AdaptiveGrid(int gridWidth, int gridHeight, Point2D start, Point2D termination, List<Point2D> attractors) {
		this.quadtree = new BalancedQuadtree(gridWidth, gridHeight);
		
		QuadtreeNode startNode = quadtree.setStart(start.getX(), start.getY());
		candidates.addAll(quadtree.checkCandidate(startNode));
		quadtree.setTermination(termination.getX(), termination.getY());
		addAttractionPoints(attractors);
		
		lightningTree = new LightningTree(startNode);
	}
	
	public void display(GL2 gl) {
		gl.glPushMatrix();
		gl.glTranslated(-0.5, -0.5, 0);

		for (QuadtreeNode node : quadtree.getLeaves()) {
			if (node.type == QuadtreeNode.START) {
				quadtree.drawNode(node, 0, 0.1, 0, gl);
				continue;
			}
			if (node.isBoundary)
				quadtree.drawNode(node, 0, 0, Math.max(node.potential, 0), gl);
			else {
				quadtree.drawNode(node, Math.max(node.potential, 0), 0, 0, gl);
				//System.out.println(Math.max(node.potential, 0));
			}
		}
		quadtree.drawBoundary(gl);
		lightningTree.drawTree(gl);

		gl.glPopMatrix();
		
	}
	
	/**
	 * 
	 * @return true if a leaf corresponding to the most fine-grained size is added in simulation.
	 */
	public boolean addLeaf() {
		if (numNewParticlesBeforeSolve == SimulationConstants.SKIP) {
			numNewParticlesBeforeSolve = 0;
			quadtree.solve();
		} else {
			numNewParticlesBeforeSolve++;
		}
		
		if (candidates.isEmpty()) {
			System.out.println("No more particle can be added.");
			return false;
		}
		
		// probability distribution is proportional to potential, normalized by totalPotential
		List<Double> probDist = new ArrayList<>();
		double totalPotential = 0;                    
		int idxChosen = 0;
		
		for (QuadtreeNode candidate : candidates) {
			if (candidate.isCandidate) {
				probDist.add(Math.pow(Math.max(0, candidate.potential), SimulationConstants.ETA));
				totalPotential += Math.pow(Math.max(0, candidate.potential), SimulationConstants.ETA);
			} else {
				probDist.add(0.0);
			}
		}
		
		if (totalPotential < EPS) {
			System.out.println("Brownian at current step. Total potential is too small: " + totalPotential);
			idxChosen = (int) (Math.random() * candidates.size());
		} else {
			double potentialSampleSum = probDist.get(0) / totalPotential;
			double random = Math.random();
			while (potentialSampleSum < random && idxChosen < candidates.size() - 1) {
				idxChosen++;
				potentialSampleSum += probDist.get(idxChosen) / totalPotential;
			}
		}
		candidates.get(idxChosen).isBoundary = true;
		// is part of the lightning, potential drops to 0
		candidates.get(idxChosen).potential = 0;
		candidates.get(idxChosen).type = QuadtreeNode.START;
		
		QuadtreeNode addedNode = candidates.get(idxChosen);
		addedNode.populateNeighbors();
		// find a neighbor that is also part of the lightning for connecting particles in render
		QuadtreeNode neighborChosen = null;
		for (QuadtreeNode neighbor : addedNode.getAllNeighbors()) {
			if (neighbor.type == QuadtreeNode.START) {
				neighborChosen = neighbor;
				break;
			}
		}
		if (neighborChosen == null) {
			System.err.println("Newly added particle does not have neighbors that are part of the lightning.");
		}
		lightningTree.addEdge(neighborChosen, addedNode);

		quadtree.insert(addedNode.midX, addedNode.midY);

		candidates.addAll(quadtree.checkCandidate(addedNode));
		if (candidates.size() > SimulationConstants.MAX_CANDIDATES_SIZE) {
			for (int i = 0; i < candidates.size() - SimulationConstants.MAX_CANDIDATES_SIZE; i++) {
				candidates.remove(i);
			}
		}
		
		terminated = terminated(addedNode);
		if (terminated) {
			lightningTree.analyzeSaliency();
		}
		return true;
	}
	
	private boolean terminated(QuadtreeNode node) {
		boolean terminated = false;
		
		for (QuadtreeNode neighbor : node.neighbors) {
			if (neighbor != null && neighbor.type == QuadtreeNode.TERMINATE) {
				terminated = true;
				break;
			}
		}
		if (terminated) {
			System.out.println("SIMULATION FINISHED SUCCESSULLY!");
		}
		return terminated;
	}
	
	public boolean hasTerminated() {
		return terminated;
	}
	
	public void addAttractionPoints(List<Point2D> attractionPoints) {
		for (Point2D p : attractionPoints) {
			quadtree.setAttraction(p.getX(), p.getY());
		}
	}

}
