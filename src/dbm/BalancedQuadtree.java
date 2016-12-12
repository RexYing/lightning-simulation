package dbm;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.xml.soap.Node;

import com.jogamp.opengl.GL2;

public class BalancedQuadtree {

	/** Node type */
	private static final int DEFAULT = 0;
	private static final int START = 1;
	private static final int TERMINATE = 2;
	private static final int ATTRACT = 3;

	QuadtreeNode root;
	private List<QuadtreeNode> finestLeaves = new ArrayList<>();

	private int maxDepth;

	public BalancedQuadtree(int gridWidth, int gridHeight) {
		root = new QuadtreeNode(null, 0, 1, 1, 0, 0, 0);
		root.subdivide();

		maxDepth = (int) Math.ceil(Math.log(Math.max(gridWidth, gridHeight)) / Math.log(2));
		System.out.println("Max depth   " + maxDepth);
	}

	public void drawBoundary(GL2 gl) {
		drawBoundary(root, gl);
	}

	public void drawBoundary(QuadtreeNode node, GL2 gl) {
		gl.glColor4d(0.1, 0.1, 0.1, 0.1);

		gl.glBegin(GL2.GL_LINE_STRIP);
		gl.glVertex2d(node.rightX, 1 - node.topY);
		gl.glVertex2d(node.rightX, 1 - node.bottomY);
		gl.glVertex2d(node.leftX, 1 - node.bottomY);
		gl.glVertex2d(node.leftX, 1 - node.topY);
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
		gl.glVertex2d(node.rightX, 1 - node.topY);
		gl.glVertex2d(node.rightX, 1 - node.bottomY);
		gl.glVertex2d(node.leftX, 1 - node.bottomY);
		gl.glVertex2d(node.leftX, 1 - node.topY);
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
				setAttraction(child);
			}
		}

		enforceBalance(currNode);

		return currNode;
	}

	/**
	 * Enforce that the quadtree is balanced after the node is inserted.
	 */
	private void enforceBalance(QuadtreeNode node) {
		for (int dir : QuadtreeNode.NEIGHBOR_DIRS) {
			QuadtreeNode neighbor = node.getNeighbor(dir);
			if (neighbor != null) {
				while (neighbor.depth < maxDepth) {
					neighbor.subdivide();
					neighbor = node.getNeighbor(dir);
				}
				finestLeaves.addAll(neighbor.parent.children);
				for (QuadtreeNode child : neighbor.parent.children) {
					setAttraction(child);
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
					setAttraction(neighborTopLeft);
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
					setAttraction(neighborTopRight);
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
					setAttraction(neighborBottomLeft);
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
					setAttraction(neighborBottomRight);
				}
			}
		}
	}
	
	private void checkCandidate(Node node) {
		
	}

	private void setAttraction(QuadtreeNode node) {
		if (Math.random() < 0.3) {
			node.isBoundary = true;
			node.potential = 0.5;
			node.isAttractor = true;
			node.isCandidate = true;
		}
	}

	public void setStart(double x, double y) {
		QuadtreeNode startNode = insert(x, y);
		startNode.isBoundary = true;
		startNode.potential = 0.0;
		startNode.isCandidate = true;
		startNode.type = START;
	}

	public void setTermination(double x, double y) {
		QuadtreeNode terminateNode = insert(x, y);
		terminateNode.isBoundary = true;
		terminateNode.potential = 1;
		terminateNode.isCandidate = true;
		terminateNode.type = TERMINATE;
	}
}
