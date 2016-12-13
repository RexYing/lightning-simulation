package dbm;

import java.util.ArrayList;
import java.util.List;

/**
 * Numerical solver for Poisson PDE on the balanced quadtree, using Incomplete
 * Cholesky Conjugate Gradient.
 * 
 * @author rex
 *
 */
public class PoissonEqSolver {

	private static final double EPS = 1e-7;

	private List<QuadtreeNode> leaves;
	private List<Double> residuals;
	private int iterations;
	private List<Double> qCG;

	public PoissonEqSolver(List<QuadtreeNode> leaves, int iterations) {
		this.leaves = leaves;
		this.iterations = iterations;
		residuals = new ArrayList<>();
		qCG = new ArrayList<>();
		for (int i = 0; i < leaves.size(); i++) {
			qCG.add(0.0);
		}
	}

	public int solve() {
		int iter = 0;

		for (QuadtreeNode node : leaves) {
			node.computeStencil();
			if (node.type == QuadtreeNode.TERMINATE) {
				System.out.println("TERMINATOR in solve  " + node.isBoundary);
			}
		}

		assignIndex();

		residual();
		List<Double> directions = new ArrayList<>(residuals);

		double deltaNew = 0;

		for (int i = 0; i < leaves.size(); i++) {
			deltaNew += residuals.get(i) * residuals.get(i);
		}

		//double deltaInit = deltaNew;
		// for determining convergence
		double maxR = 2 * EPS;

		while (iter < iterations && maxR > EPS) {
			// q = Ad
			for (int j = 0; j < leaves.size(); j++) {
				QuadtreeNode node = leaves.get(j);
				double neighborSum = 0;
				for (int dir = 0; dir < 4; dir++) {
					neighborSum += directions.get(node.neighbors.get(2 * dir).idx) * node.stencil.get(2 * dir);
					if (node.neighbors.get(2 * dir + 1) != null) {
						neighborSum += directions.get(node.neighbors.get(2 * dir + 1).idx)
								* node.stencil.get(2 * dir + 1);
					}
				}
				qCG.set(j, -neighborSum + directions.get(j) * node.stencil.get(8));
			}

			// alpha: the step size at current iteration
			// alpha = deltaNew / (transpose(d) * q)
			double alpha = 0;
			for (int i = 0; i < directions.size(); i++) {
				alpha += directions.get(i) * qCG.get(i);
			}
			if (Math.abs(alpha) > EPS) {
				alpha = deltaNew / alpha;
			}

			// Update x in direction specified by directions by amount alpha
			// x = x + alpha * d
			for (int i = 0; i < leaves.size(); i++) {
				leaves.get(i).potential += alpha * directions.get(i);
			}

			// Update residual
			// r = r - alpha * q
			maxR = 0;
			for (int i = 0; i < leaves.size(); i++) {
				double residual = residuals.get(i) - alpha * qCG.get(i);
				residuals.set(i, residual);
				if (Math.abs(residual) > maxR) {
					maxR = Math.abs(residual);
				}
			}

			double deltaOld = deltaNew;

			// Update delta to the 2-norm of residual
			deltaNew = 0;
			for (double res : residuals) {
				deltaNew += res * res;
			}

			// beta = deltaNew / deltaOld
			double beta = deltaNew / deltaOld;

			// Update directions
			// d = r + beta * d
			for (int i = 0; i < directions.size(); i++) {
				directions.set(i, residuals.get(i) + beta * directions.get(i));
			}
			iter++;
		}

		return iter;
	}

	private void assignIndex() {
		for (int i = 0; i < leaves.size(); i++) {
			leaves.get(i).idx = i;
		}
	}

	private double residual() {
		double maxRes = 0;

		for (QuadtreeNode node : leaves) {
			double neighborSum = 0;

			for (int dir = 0; dir < 4; dir++) {
				neighborSum += node.neighbors.get(2 * dir).potential * node.stencil.get(2 * dir);
				if (node.neighbors.get(2 * dir + 1) != null) {
					neighborSum += node.neighbors.get(2 * dir + 1).potential * node.stencil.get(2 * dir + 1);
				}
			}
			double residual = node.rhs - (-neighborSum + node.potential * node.stencil.get(8));
			residuals.add(residual);
			if (Math.abs(residual) > maxRes) {
				maxRes = Math.abs(residual);
			}
		}
		if (residuals.size() != leaves.size()) {
			System.err.println("The size of residuals is incorrect.");
		}
		return maxRes;
	}
}
