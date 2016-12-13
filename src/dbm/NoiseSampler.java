package dbm;

import java.util.List;

import za.co.luma.geom.Vector2DDouble;
import za.co.luma.math.sampling.Sampler;
import za.co.luma.math.sampling.UniformPoissonDiskSampler;

/**
 * Interface for the Poisson disk sampler.
 * Credit of the Poisson disk sampler code is given to Dev. Mag Issue 21, http://devmag.org.za/2009/05/03/poisson-disk-sampling/
 * @author rex
 *
 */
public class NoiseSampler {

	public boolean[][] poissonDiskSample(int w, int h, int r) {
		
		boolean[][] noise = new boolean[w][h];
		
		Sampler<Vector2DDouble> sampler = new UniformPoissonDiskSampler(0, 0, w, h, r);
		List<Vector2DDouble> pointList = sampler.sample();
		
		for (Vector2DDouble point : pointList)
		{			
			noise[(int) point.x][(int) point.y] = true;
		}		
		return noise;
	}
	
	public static void main(String[] args) {
		new NoiseSampler().poissonDiskSample(128, 128, 5);
	}
}
