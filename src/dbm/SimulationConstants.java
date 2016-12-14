package dbm;

public class SimulationConstants {

	static final int WIDTH = 128;
	static final int HEIGHT = 128;
	
	/** For conjugate gradient method */
	static final int FIRST_TIME_SOLVE_ITERATIONS = 10000;
	static final int INTERATIONS = 50;
	/** Size of candidates from which to choose growth site */
	static final int MAX_CANDIDATES_SIZE = 50;
	/** Number of particles to add before solving Poisson equation again */
	static final int SKIP = 2;
	
	/** Explained in paper: the power of potential in the formula of distribution for choosing next particle */
	static final double ETA = 3;
	
	static final double ATTRACTOR_POTENTIAL = 0.5;
	
}
