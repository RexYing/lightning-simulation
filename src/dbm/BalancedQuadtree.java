package dbm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.GL2;

public class BalancedQuadtree {

	

	QuadtreeNode root;
	private List<QuadtreeNode> finestLeaves = new ArrayList<>();

	private int maxDepth;
	
	private boolean solveFirstTime = true;
	private boolean[][] noise;

	public BalancedQuadtree(int gridWidth, int gridHeight) {
		root = new QuadtreeNode(null, 0, 1, 1, 0, 0, 0);
		root.subdivide();

		maxDepth = (int) Math.ceil(Math.log(Math.max(gridWidth, gridHeight)) / Math.log(2));
		noise = new boolean[gridWidth][gridHeight];
		//noise = new NoiseSampler().poissonDiskSample(gridWidth, gridHeight, 5);
		System.out.println("Max depth   " + maxDepth);
	}

	public void drawBoundary(GL2 gl) {
		drawBoundary(root, gl);
	}

	public void drawBoundary(QuadtreeNode node, GL2 gl) {
		gl.glColor4d(0.2, 0.2, 0.2, 0.3);

		gl.glBegin(GL2.GL_LINE_STRIP);
		gl.glVertex2d(node.rightX, node.topY);
		gl.glVertex2d(node.rightX, node.bottomY);
		gl.glVertex2d(node.leftX, node.bottomY);
		gl.glVertex2d(node.leftX, node.topY);
		gl.glEnd();

		if (!node.isLeaf) {
			for (QuadtreeNode child : node.children) {
				drawBoundary(child, gl);
			}
		}
	}

	public void drawNode(QuadtreeNode node, double r, double g, double b, GL2 gl) {

		gl.glColor4d(r, g, b, 1);
		gl.glBegin(GL2.GL_QUADS);
		gl.glVertex2d(node.rightX, node.topY);
		gl.glVertex2d(node.rightX, node.bottomY);
		gl.glVertex2d(node.leftX, node.bottomY);
		gl.glVertex2d(node.leftX, node.topY);
		gl.glEnd();
	}

	public List<QuadtreeNode> getLeaves() {
		return getLeaves(root);
	}

	public List<QuadtreeNode> getLeaves(QuadtreeNode node) {
		List<QuadtreeNode> leaves = new ArrayList<>();
		if (node.isLeaf) {
			leaves.add(node);
		} else {
			for (QuadtreeNode child : node.children) {
				leaves.addAll(getLeaves(child));
			}
		}
		return leaves;
	}
	
	private List<QuadtreeNode> getInteriorLeaves() {
		return getInteriorLeaves(root);
	}
	
	private List<QuadtreeNode> getInteriorLeaves(QuadtreeNode node) {
		List<QuadtreeNode> leaves = new ArrayList<>();
		if (node.isLeaf) {
			if (!node.isBoundary) {
				leaves.add(node);
			}
		} else {
			for (QuadtreeNode child : node.children) {
				leaves.addAll(getInteriorLeaves(child));
			}
		}
		return leaves;
	}

	/**
	 * Get the leaves at max resolution.
	 */
	public List<QuadtreeNode> getFinestLeaves() {
		return finestLeaves;
	}

	/**
	 * Insert point (x, y) into quadtree at max level.
	 */
	public QuadtreeNode insert(double x, double y) {
		int currDepth = 0;
		QuadtreeNode currNode = root;
		boolean existed = true;

		while (currDepth < maxDepth) {
			double diffX = x - currNode.midX;
			double diffY = y - currNode.midY;

			// the index of the child of current node that point (x, y) belongs
			// to
			int childIdx = 1;
			if (diffX > 0) {
				if (diffY < 0)
					childIdx = 2;
			} else if (diffY < 0) {
				childIdx = 3;
			} else {
				childIdx = 0;
			}

			// check if that child of the current node exists
			if (currNode.children.isEmpty() || currNode.children.get(childIdx) == null) {
				existed = false;
				currNode.subdivide();
			}

			currNode = currNode.children.get(childIdx);
			currDepth++;
		}

		if (!existed) {
			finestLeaves.addAll(currNode.parent.children);
			for (QuadtreeNode child : currNode.parent.children) {
				generateNoiseAttraction(child);
			}
		}

		enforceSameDepth(currNode);
		
		balanceQuadtree();

		return currNode;
	}

	/**
	 * Enforce that the depth of neighboring nodes of the inserted nodes are all
	 * of maxDepth
	 */
	private void enforceSameDepth(QuadtreeNode node) {
		for (int dir : QuadtreeNode.NEIGHBOR_DIRS) {
			QuadtreeNode neighbor = node.getNeighbor(dir);
			if (neighbor != null) {
				while (neighbor.depth < maxDepth) {
					neighbor.subdivide();
					neighbor = node.getNeighbor(dir);
				}
				finestLeaves.addAll(neighbor.parent.children);
				for (QuadtreeNode child : neighbor.parent.children) {
					generateNoiseAttraction(child);
				}
			}
		}

		QuadtreeNode neighborAbove = node.getNeighborAbove();
		if (neighborAbove != null) {
			QuadtreeNode neighborTopLeft = neighborAbove.getNeighborLeft();
			if (neighborTopLeft != null) {
				while (neighborTopLeft.depth < maxDepth) {
					neighborTopLeft.subdivide();
					neighborTopLeft = neighborTopLeft.children.get(2);
				}
				finestLeaves.addAll(neighborTopLeft.parent.children);
				for (QuadtreeNode child : neighborTopLeft.parent.children) {
					generateNoiseAttraction(child);
				}
			}

			QuadtreeNode neighborTopRight = neighborAbove.getNeighborRight();
			if (neighborTopRight != null) {
				while (neighborTopRight.depth < maxDepth) {
					neighborTopRight.subdivide();
					neighborTopRight = neighborTopRight.children.get(3);
				}
				finestLeaves.addAll(neighborTopRight.parent.children);
				for (QuadtreeNode child : neighborTopRight.parent.children) {
					generateNoiseAttraction(child);
				}
			}
		}

		QuadtreeNode neighborBelow = node.getNeighborBelow();
		if (neighborBelow != null) {
			QuadtreeNode neighborBottomLeft = neighborBelow.getNeighborLeft();
			if (neighborBottomLeft != null) {
				while (neighborBottomLeft.depth < maxDepth) {
					neighborBottomLeft.subdivide();
					neighborBottomLeft = neighborBottomLeft.children.get(1);
				}
				finestLeaves.addAll(neighborBottomLeft.parent.children);
				for (QuadtreeNode child : neighborBottomLeft.parent.children) {
					generateNoiseAttraction(child);
				}
			}

			QuadtreeNode neighborBottomRight = neighborBelow.getNeighborRight();
			if (neighborBottomRight != null) {
				while (neighborBottomRight.depth < maxDepth) {
					neighborBottomRight.subdivide();
					neighborBottomRight = neighborBottomRight.children.get(0);
				}
				finestLeaves.addAll(neighborBottomRight.parent.children);
				for (QuadtreeNode child : neighborBottomRight.parent.children) {
					generateNoiseAttraction(child);
				}
			}
		}
	}

	public List<QuadtreeNode> checkCandidate(QuadtreeNode node) {
		List<QuadtreeNode> candidates = new ArrayList<>();

		QuadtreeNode neighborAbove = node.getNeighborAbove();
		if (neighborAbove != null && neighborAbove.depth == maxDepth) {
			if (!neighborAbove.isCandidate) {
				candidates.add(neighborAbove);
				neighborAbove.isCandidate = true;
			}

			QuadtreeNode neighborTopLeft = neighborAbove.getNeighborLeft();
			if (neighborTopLeft != null && !neighborTopLeft.isCandidate) {
				candidates.add(neighborTopLeft);
				neighborTopLeft.isCandidate = true;
			}

			QuadtreeNode neighborTopRight = neighborAbove.getNeighborLeft();
			if (neighborTopRight != null && !neighborTopRight.isCandidate) {
				candidates.add(neighborTopRight);
				neighborTopRight.isCandidate = true;
			}
		}

		QuadtreeNode neighborBelow = node.getNeighborBelow();
		if (neighborBelow != null && neighborBelow.depth == maxDepth) {
			if (!neighborBelow.isCandidate) {
				candidates.add(neighborBelow);
				neighborBelow.isCandidate = true;
			}

			QuadtreeNode neighborBottomLeft = neighborBelow.getNeighborLeft();
			if (neighborBottomLeft != null && !neighborBottomLeft.isCandidate) {
				candidates.add(neighborBottomLeft);
				neighborBottomLeft.isCandidate = true;
			}

			QuadtreeNode neighborBottomRight = neighborBelow.getNeighborLeft();
			if (neighborBottomRight != null && !neighborBottomRight.isCandidate) {
				candidates.add(neighborBottomRight);
				neighborBottomRight.isCandidate = true;
			}
		}

		QuadtreeNode neighborLeft = node.getNeighborLeft();
		if (neighborLeft != null && !neighborLeft.isCandidate) {
			candidates.add(neighborLeft);
			neighborLeft.isCandidate = true;
		}

		QuadtreeNode neighborRight = node.getNeighborRight();
		if (neighborRight != null && !neighborRight.isCandidate) {
			candidates.add(neighborRight);
			neighborRight.isCandidate = true;
		}

		return candidates;
	}

	private void balanceQuadtree() {
		List<QuadtreeNode> leaves = getLeaves();

		int idx = 0;
		while (idx < leaves.size()) {
			QuadtreeNode node = leaves.get(idx);
			for (int dir : QuadtreeNode.NEIGHBOR_DIRS) {
				QuadtreeNode neighbor = node.getNeighbor(dir);
				if (neighbor != null) {
					while (neighbor.depth < node.depth - 1) {
						neighbor.subdivide();
						leaves.addAll(neighbor.children);
						neighbor = node.getNeighbor(dir);
					}
				}
			}
			idx++;
		}
	}
	
	private void buildNeighbors() {
		List<QuadtreeNode> leaves = getLeaves();
		
		for (QuadtreeNode node : leaves) {
			node.populateNeighbors();
		}
	}
	
	public int solve() {
		balanceQuadtree();
		buildNeighbors();
		
		List<QuadtreeNode> leaves = getInteriorLeaves();
		
		int numIterations = SimulationConstants.INTERATIONS;
		if (solveFirstTime) {
			numIterations = SimulationConstants.FIRST_TIME_SOLVE_ITERATIONS;
			solveFirstTime = false;
		}
		PoissonEqSolver solver = new PoissonEqSolver(leaves, numIterations);
		solver.solveCRS();
		//return solver.solve();
		return 0;
	}

	private void generateNoiseAttraction(QuadtreeNode node) {
		if (node.type != QuadtreeNode.DEFAULT) {
			return;
		}
		int x = (int) node.midX * (1 << maxDepth);
		int y = (int) node.midY * (1 << maxDepth);
		if (noise[x][y]) {
			node.isBoundary = true;
			node.potential = 0.5;
			node.isAttractor = true;
			node.isCandidate = false;
			node.type = QuadtreeNode.ATTRACT;
			System.out.println("NOISE");
		}
	}
	
	public QuadtreeNode setAttraction(double x, double y) {
		QuadtreeNode attractNode = insert(x, y);
		if (attractNode.type != QuadtreeNode.DEFAULT) {
			System.out.println("An attraction point is already a start/terminating point.");
			return attractNode;
		}
		attractNode.isBoundary = true;
		attractNode.potential = SimulationConstants.ATTRACTOR_POTENTIAL;
		attractNode.isCandidate = false;
		attractNode.type = QuadtreeNode.ATTRACT;
		return attractNode;
	}

	public QuadtreeNode setStart(double x, double y) {
		QuadtreeNode startNode = insert(x, y);
		startNode.isBoundary = true;
		startNode.potential = 0.0;
		startNode.isCandidate = true;
		startNode.type = QuadtreeNode.START;
		return startNode;
	}

	public QuadtreeNode setTermination(double x, double y) {
		QuadtreeNode terminateNode = insert(x, y);
		terminateNode.isBoundary = true;
		terminateNode.potential = 1;
		terminateNode.isCandidate = true;
		terminateNode.type = QuadtreeNode.TERMINATE;
		return terminateNode;
	}

	public int getMaxDepth() {
		return maxDepth;
	}
}
