package dbm;

public class SimulationConstants {

	static final int WIDTH = 128;
	static final int HEIGHT = 128;
	
	static final int FIRST_TIME_SOLVE_ITERATIONS = 10000;
	static final int INTERATIONS = 10;
	/** Number of particles to add before solving Poisson equation again */
	static final int SKIP = 2;
	
	/** Explained in paper: the power of potential in the formula of distribution for choosing next particle */
	static final double ETA = 2;
}
