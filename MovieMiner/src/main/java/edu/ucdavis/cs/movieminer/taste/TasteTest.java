/**
 * 
 */
package edu.ucdavis.cs.movieminer.taste;

import java.io.File;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.eval.RecommenderBuilder;
import com.planetj.taste.eval.RecommenderEvaluator;
import com.planetj.taste.impl.correlation.AveragingPreferenceInferrer;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.eval.RMSRecommenderEvaluator;
import com.planetj.taste.impl.model.netflix.NetflixDataModel;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.CachingRecommender;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

/**
 * 
 * @author pfishero
 * @version $Id$
 */
public class TasteTest {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println("Usage:");
			System.out.println(" java TasteTest {netflix_data_dir} {k_neighbor_value}");
			System.exit(-1);
		}
		
		final String netflixDataDir = args[0];
		final int neighbors = Integer.parseInt(args[1]);
		System.out.println("netflixDataDir="+netflixDataDir+" neighbors="+neighbors);
		
		final DataModel myModel = new NetflixDataModel(new File(netflixDataDir));

		RecommenderBuilder builder = new RecommenderBuilder() {
		    public Recommender buildRecommender(DataModel model) throws TasteException {
		    	// build and return the Recommender to evaluate here
				UserCorrelation userCorrelation = new PearsonCorrelation(model);
				// Optional:
				userCorrelation
						.setPreferenceInferrer(new AveragingPreferenceInferrer(model));

				UserNeighborhood neighborhood = new NearestNUserNeighborhood(neighbors,
						userCorrelation, model);

				Recommender recommender = new GenericUserBasedRecommender(model,
						neighborhood, userCorrelation);
				Recommender cachingRecommender = new CachingRecommender(recommender);
				
				return cachingRecommender;
		    }
		  };
		RecommenderEvaluator evaluator = new RMSRecommenderEvaluator();
		double evaluation = evaluator.evaluate(builder, myModel, 0.9, 1.0);
		System.out.println("evaluation = "+evaluation);
	}

}
