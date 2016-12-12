package dbm;

import java.util.List;

/**
 * Numerical solver for Poisson equation on the balanced quadtree.
 * @author rex
 *
 */
public class PoissonEqSolver {
	
	private List<QuadtreeNode> leaves;
	private int iterations;

	public PoissonEqSolver(List<QuadtreeNode> leaves, int iterations) {
		this.leaves = leaves;
		this.iterations = iterations;
	}
	
	public int solve() {
		int iter = 0;
		
		return iter;
	}
}
