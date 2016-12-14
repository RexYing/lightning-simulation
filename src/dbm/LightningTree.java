package dbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.ListSelectionEvent;

import com.jogamp.opengl.GL2;

/**
 * A tree that represents the lightning.
 * Add jittering to avoid regularities of grid discretization.
 * @author rexyi
 *
 */
public class LightningTree {

	/** Maps quadtree smallest leaf nodes to the nodes of lightning tree */
	private Map<QuadtreeNode, TreeNode> nodeMap = new HashMap<>();
	
	private TreeNode root;
	
	private TreeNode terminatingNode;
	
	private boolean saliencyComputed = false;
	
	private int[] strokeWidth = {8, 5, 3, 2, 1};
	
	class TreeNode {
		double x;
		double y;
		
		TreeNode parent;
		List<TreeNode> children = new ArrayList<>();
		
		/** Main branch has saliency 0, with the most salient line segments. 
		 * The secondary branches have saliency 1.
		 * The rest of the branches have saliency 2. 
		 */
		int saliency = 2;
		
		public TreeNode(QuadtreeNode quadtreeNode, QuadtreeNode parentNode) {
			parent = nodeMap.get(parentNode);
			// jitter
			double minX = quadtreeNode.leftX;
			double maxX = quadtreeNode.rightX;
			this.x = minX + Math.random() * (maxX - minX) / 2 + (maxX - minX) / 2;
			double minY = quadtreeNode.bottomY;
			double maxY = quadtreeNode.topY;
			this.y = minY + Math.random() * (maxY - minY);
		}
		
		void addChild(TreeNode child) {
			children.add(child);
		}
	}
	
	public LightningTree(QuadtreeNode startNode) {
		root = new TreeNode(startNode, null);
		nodeMap.put(startNode, root);
	}
	
	public void addEdge(QuadtreeNode parentNode, QuadtreeNode childNode) {
		TreeNode parentTreeNode = nodeMap.get(parentNode);
		TreeNode childTreeNode = new TreeNode(childNode, parentNode);
		nodeMap.put(childNode, childTreeNode);
		parentTreeNode.addChild(childTreeNode);
		terminatingNode = childTreeNode;
	}
	
	public void drawTree(GL2 gl) {
		drawTreeNode(root, gl);
	}
	
	public void drawTreeNode(TreeNode node, GL2 gl) {
		for (TreeNode child : node.children) {
			if (saliencyComputed) {
				gl.glColor4d(0, 0, Math.pow(2, -(node.saliency + child.saliency)), 1);
				gl.glLineWidth(strokeWidth[node.saliency + child.saliency]);
			} else {
				gl.glColor4d(0.0, 0.0, 1, 0);
				gl.glLineWidth(3);
			}
			gl.glBegin(GL2.GL_LINES);
			gl.glVertex2d(node.x, node.y);
			gl.glVertex2d(child.x, child.y);
			gl.glEnd();
			drawTreeNode(child, gl);
		}
		gl.glLineWidth(1);
	}
	
	public void analyzeSaliency() {
		List<TreeNode> mainBranch = new ArrayList<>();
		TreeNode currNode = terminatingNode;
		mainBranch.add(terminatingNode);
		while (currNode != root) {
			currNode = currNode.parent;
			mainBranch.add(currNode);
		}
		Collections.reverse(mainBranch);
		for (int i = 0; i < mainBranch.size() - 1; i++) {
			TreeNode node = mainBranch.get(i);
			node.saliency = 0;
			TreeNode mainChild = mainBranch.get(i + 1);
			for (TreeNode child : node.children) {
				if (child == mainChild) {
					continue;
				}
				analyzeSaliencySecondary(child, 1);
			}
		}
		saliencyComputed = true;
	}
	
	private void analyzeSaliencySecondary(TreeNode node, int saliency) {
		List<TreeNode> longestBranch = new ArrayList<>();
		findLongestBrachDFS(node, longestBranch);
		for (int i = 0; i < longestBranch.size(); i++) {
			longestBranch.get(i).saliency = saliency;
		}
	}
	
	private int findLongestBrachDFS(TreeNode node, List<TreeNode> branch) {
		int maxDepth = 0;
		branch.add(node);
		List<TreeNode> longestBranch = new ArrayList<>();
		for (TreeNode child : node.children) {
			List<TreeNode> newBranch = new ArrayList<>();
			int depth = findLongestBrachDFS(child, newBranch);
			if (maxDepth < depth) {
				maxDepth = depth;
				longestBranch = newBranch;
			}
		}
		branch.addAll(longestBranch);
		return maxDepth + 1;
	}
	
	public int numParticles() {
		return nodeMap.size();
	}
}
