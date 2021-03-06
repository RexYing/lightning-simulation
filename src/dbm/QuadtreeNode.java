package dbm;

import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.StyledEditorKit.ForegroundAction;

class QuadtreeNode {
	
	/** Node type */
	static final int DEFAULT = 0;
	static final int START = 1;
	static final int TERMINATE = 2;
	static final int ATTRACT = 3;

	private static final int TOP_NEIGHBOR = 0;
	private static final int BOTTOM_NEIGHBOR = 1;
	private static final int LEFT_NEIGHBOR = 2;
	private static final int RIGHT_NEIGHBOR = 3;

	static final int[] NEIGHBOR_DIRS = { TOP_NEIGHBOR, BOTTOM_NEIGHBOR, LEFT_NEIGHBOR, RIGHT_NEIGHBOR };

	QuadtreeNode parent;
	/**
	 * Either empty or a list of 4 children: top left, top right, bottom right,
	 * bottom left.
	 */
	List<QuadtreeNode> children;

	boolean isLeaf;

	/**
	 * The top left coordinate of this node in the grid. The bottom left is (0,
	 * 0).
	 */
	double leftX;
	double rightX;
	double topY;
	double bottomY;
	double midX;
	double midY;

	/**
	 * A list of (potential) 8 neighbors: 2 above; 2 on right; 2 below; 2 on
	 * left.
	 */
	List<QuadtreeNode> neighbors;
	/**
	 * A list of (potential) 9-node stencil including neighboring nodes and
	 * itself.
	 */
	List<Double> stencil;

	/** depth of current node in the quadtree. */
	int depth;
	/**
	 * a linear ordering of nodes in the quadtree for solving matrix system.
	 */
	int idx;

	boolean isBoundary;
	boolean isAttractor = false;
	/** To be considered for growth site */
	boolean isCandidate = false;

	double potential = 0;
	/**  right-hand side of linear system */
	double rhs = 0;

	int type = 0;

	QuadtreeNode(QuadtreeNode parent, double leftX, double rightX, double topY, double bottomY, int depth,
			double potential) {
		this.parent = parent;
		this.depth = depth;
		this.leftX = leftX;
		this.rightX = rightX;
		this.midX = (leftX + rightX) / 2;
		this.topY = topY;
		this.bottomY = bottomY;
		this.midY = (topY + bottomY) / 2;
		this.potential = potential;
		this.isLeaf = true;
		this.isBoundary = false;
		initLists();
	}

	QuadtreeNode(int depth) {
		this.parent = null;
		this.depth = depth;
		this.isBoundary = true;
		this.isLeaf = true;
		initLists();
	}
	
	private void initLists() {
		stencil = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			stencil.add(0.0);
		}
		children = new ArrayList<>();
		neighbors = new ArrayList<>();
	}

	void subdivide() {
		if (!children.isEmpty()) {
			System.err.println("Attempt to subdivide a node that already has children.");
			return;
		}

		children.add(new QuadtreeNode(this, leftX, midX, topY, midY, depth + 1, potential));
		children.add(new QuadtreeNode(this, midX, rightX, topY, midY, depth + 1, potential));
		children.add(new QuadtreeNode(this, midX, rightX, midY, bottomY, depth + 1, potential));
		children.add(new QuadtreeNode(this, leftX, midX, midY, bottomY, depth + 1, potential));
		isLeaf = false;
	}

	QuadtreeNode getNeighborAbove() {
		if (parent == null)
			return null;

		// if it is the southern child of the parent
		if (parent.children.get(3) == this)
			return parent.children.get(0);
		if (parent.children.get(2) == this)
			return parent.children.get(1);

		// else look up higher
		QuadtreeNode pneighbor = parent.getNeighborAbove();

		if (pneighbor == null || pneighbor.children.isEmpty()) {
			return pneighbor;
		} else if (parent.children.get(0) == this) {
			// if it is the upper left child of the parent, the neighbor above
			// should be the bottom left child of pneighbor
			return pneighbor.children.get(3);
		} else {
			// if it is the NE child of the parent
			return pneighbor.children.get(2);
		}
	}

	QuadtreeNode getNeighborBelow() {
		if (parent == null)
			return null;

		if (parent.children.get(0) == this)
			return parent.children.get(3);
		if (parent.children.get(1) == this)
			return parent.children.get(2);

		QuadtreeNode pneighbor = parent.getNeighborBelow();

		if (pneighbor == null || pneighbor.children.isEmpty()) {
			return pneighbor;
		} else if (parent.children.get(3) == this) {
			return pneighbor.children.get(0);
		} else {
			return pneighbor.children.get(1);
		}
	}

	QuadtreeNode getNeighborLeft() {
		if (parent == null)
			return null;

		if (parent.children.get(1) == this)
			return parent.children.get(0);
		if (parent.children.get(2) == this)
			return parent.children.get(3);

		QuadtreeNode pneighbor = parent.getNeighborLeft();

		if (pneighbor == null || pneighbor.children.isEmpty()) {
			return pneighbor;
		} else if (parent.children.get(0) == this) {
			return pneighbor.children.get(1);
		} else {
			return pneighbor.children.get(2);
		}
	}

	QuadtreeNode getNeighborRight() {
		if (parent == null)
			return null;

		if (parent.children.get(0) == this)
			return parent.children.get(1);
		if (parent.children.get(3) == this)
			return parent.children.get(2);

		QuadtreeNode pneighbor = parent.getNeighborRight();

		if (pneighbor == null || pneighbor.children.isEmpty()) {
			return pneighbor;
		} else if (parent.children.get(1) == this) {
			return pneighbor.children.get(0);
		} else {
			return pneighbor.children.get(3);
		}
	}

	QuadtreeNode getNeighbor(int dir) {
		switch (dir) {
		case TOP_NEIGHBOR:
			return getNeighborAbove();
		case BOTTOM_NEIGHBOR:
			return getNeighborBelow();
		case LEFT_NEIGHBOR:
			return getNeighborLeft();
		case RIGHT_NEIGHBOR:
			return getNeighborRight();
		default:
			throw new RuntimeException("Undefined neighbor direction constant.");
		}
	}

	void populateNeighbors() {
		neighbors = new ArrayList<>();
		
		QuadtreeNode neighborAbove = getNeighborAbove();
		if (neighborAbove != null) {
			if (neighborAbove.children.isEmpty()) {
				neighbors.add(neighborAbove);
				neighbors.add(null);
			} else {
				neighbors.add(neighborAbove.children.get(3));
				neighbors.add(neighborAbove.children.get(2));
			}
		} else {
			neighbors.add(new QuadtreeNode(depth));
			neighbors.add(null);
		}
		
		QuadtreeNode neighborRight = getNeighborRight();
		if (neighborRight != null) {
			if (neighborRight.children.isEmpty()) {
				neighbors.add(neighborRight);
				neighbors.add(null);
			} else {
				neighbors.add(neighborRight.children.get(0));
				neighbors.add(neighborRight.children.get(3));
			}
		} else {
			neighbors.add(new QuadtreeNode(depth));
			neighbors.add(null);
		}
		
		QuadtreeNode neighborBelow = getNeighborBelow();
		if (neighborBelow != null) {
			if (neighborBelow.children.isEmpty()) {
				neighbors.add(neighborBelow);
				neighbors.add(null);
			} else {
				neighbors.add(neighborBelow.children.get(1));
				neighbors.add(neighborBelow.children.get(0));
			}
		} else {
			neighbors.add(new QuadtreeNode(depth));
			neighbors.add(null);
		}
		
		QuadtreeNode neighborLeft = getNeighborLeft();
		if (neighborLeft != null) {
			if (neighborLeft.children.isEmpty()) {
				neighbors.add(neighborLeft);
				neighbors.add(null);
			} else {
				neighbors.add(neighborLeft.children.get(2));
				neighbors.add(neighborLeft.children.get(1));
			}
		} else {
			neighbors.add(new QuadtreeNode(depth));
			neighbors.add(null);
		}
		
		if (neighbors.size() != 8) {
			System.err.println("NEIGHBORS SIZE INCORRECT:  " + neighbors.size());
		}
	}

	QuadtreeNode getNeighborTopLeft() {
		QuadtreeNode neighborAbove = getNeighborAbove();
		if (neighborAbove != null) {
			return neighborAbove.getNeighborLeft();
		} else {
			return null;
		}
	}

	QuadtreeNode getNeighborTopRight() {
		QuadtreeNode neighborAbove = getNeighborAbove();
		if (neighborAbove != null) {
			return neighborAbove.getNeighborRight();
		} else {
			return null;
		}
	}

	QuadtreeNode getNeighborBottomLeft() {
		QuadtreeNode neighborBelow = getNeighborBelow();
		if (neighborBelow != null) {
			return neighborBelow.getNeighborLeft();
		} else {
			return null;
		}
	}

	QuadtreeNode getNeighborBottomRight() {
		QuadtreeNode neighborBelow = getNeighborBelow();
		if (neighborBelow != null) {
			return neighborBelow.getNeighborRight();
		} else {
			return null;
		}
	}
	
	/**
	 * Get all neighbors including diagonal neighbors
	 * @return
	 */
	List<QuadtreeNode> getAllNeighbors() {
		List<QuadtreeNode> allNeighbors = new ArrayList<>();
		for (QuadtreeNode node : neighbors) {
			if (node != null) {
				allNeighbors.add(node);
			}
		}
		QuadtreeNode topLeft = getNeighborTopLeft();
		if (topLeft != null) {
			allNeighbors.add(topLeft);
		}
		QuadtreeNode topRight = getNeighborTopRight();
		if (topRight != null) {
			allNeighbors.add(topRight);
		}
		QuadtreeNode bottomLeft = getNeighborBottomLeft();
		if (bottomLeft != null) {
			allNeighbors.add(bottomLeft);
		}
		QuadtreeNode bottomRight = getNeighborBottomRight();
		if (bottomRight != null) {
			allNeighbors.add(bottomRight);
		}
		return allNeighbors;
	}
	
	public void computeStencil() {
		
		double deltaSum = 0;
		rhs = 0;
		
		for (int i = 0; i < 4; i++) {
			
			if (neighbors.get(2 * i + 1) == null) {
				if (depth == neighbors.get(2 * i).depth) {
					deltaSum += (1 << depth);
					if (!neighbors.get(2 * i).isBoundary) {
						stencil.set(2 * i, (double)(1 << depth));
					} else {
						rhs += neighbors.get(2 * i).potential * (1 << depth);
						//System.out.println(neighbors.get(2 * i).potential + "  " + neighbors.get(2 * i).type);
					}
				} else {
					// neighbor is larger (side length differs by a factor of 2 by properties of balanced quadtree)
					deltaSum += 0.5 * (1 << depth);
					if (!neighbors.get(2 * i).isBoundary) {
						stencil.set(2 * i, 0.5 * (double)(1 << depth));
					} else {
						rhs += neighbors.get(2 * i).potential * 0.5 * (1 << depth);
						
					}
				}
			} else {
				// neighbor side length is smaller by a factor of 2
				deltaSum += 2 * (1 << depth);
				if (!neighbors.get(2 * i).isBoundary) {
					stencil.set(2 * i, (double)(1 << depth));
				} else {
					rhs += neighbors.get(2 * i).potential * (1 << depth);
				}
				if (!neighbors.get(2 * i + 1).isBoundary) {
					stencil.set(2 * i + 1, (double)(1 << depth));
				} else {
					rhs += neighbors.get(2 * i + 1).potential * (1 << depth);
				}
			}
		}
		stencil.set(8, deltaSum);
	}
}
