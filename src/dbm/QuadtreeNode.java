package dbm;

import java.util.ArrayList;
import java.util.List;

class QuadtreeNode {
	
	private static final int TOP_NEIGHBOR = 0;
	private static final int BOTTOM_NEIGHBOR = 1;
	private static final int LEFT_NEIGHBOR = 2;
	private static final int RIGHT_NEIGHBOR = 3;
	
	static final int[] NEIGHBOR_DIRS = {TOP_NEIGHBOR, BOTTOM_NEIGHBOR, LEFT_NEIGHBOR, RIGHT_NEIGHBOR};

	QuadtreeNode parent;
	/**
	 * Either empty or a list of 4 children: top left, top right, bottom right,
	 * bottom left.
	 */
	List<QuadtreeNode> children = new ArrayList<>();

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

	int type = 0;

	QuadtreeNode(QuadtreeNode parent, double leftX, double rightX, double topY, double bottomY, int depth, double potential) {
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
		}

	QuadtreeNode(int depth) {
			this.parent = null;
			this.depth = depth;
			this.isBoundary = true;

		}

	void subdivide() {
		System.out.println("divide  " + depth);
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
		if (parent == null) return null;

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
			  // if it is the upper left child of the parent, the neighbor above should be the bottom left child of pneighbor
			  return pneighbor.children.get(3);
		  } else {
			  // if it is the NE child of the parent
			  return pneighbor.children.get(2);
		  }
	}
	
	QuadtreeNode getNeighborBelow() {
		if (parent == null) return null;

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
		if (parent == null) return null;

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
		if (parent == null) return null;

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

}
