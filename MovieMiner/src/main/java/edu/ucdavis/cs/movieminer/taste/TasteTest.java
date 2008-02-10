/**
 * 
 */
package edu.ucdavis.cs.movieminer.taste;

import java.io.File;
import java.util.List;

import com.planetj.taste.impl.model.netflix.NetflixDataModel;
import com.planetj.taste.impl.recommender.CachingRecommender;
import com.planetj.taste.impl.recommender.slopeone.SlopeOneRecommender;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

/**
 * Requires the following file and directory
 * to be in the netflix_data_dir.  
 * 
 * movie_titles.txt (file)
 * training_set (directory containing movie ratings)
 * 
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
		} else {
			System.out.println("using command line args: "+args);
		}
		
		final String netflixDataDir = args[0];
		final int neighbors = Integer.parseInt(args[1]);
		System.out.println("netflixDataDir="+netflixDataDir+" neighbors="+neighbors);
		
		final DataModel myModel = new NetflixDataModel(new File(netflixDataDir));

		Recommender recommender = new SlopeOneRecommender(myModel);
		//Recommender cachingRecommender = new CachingRecommender(recommender);
		
		
		
//		RecommenderBuilder builder = new RecommenderBuilder() {
//		    public Recommender buildRecommender(DataModel model) throws TasteException {
//		    	// build and return the Recommender to evaluate here
//				UserCorrelation userCorrelation = new PearsonCorrelation(model);
//				// Optional:
//				userCorrelation
//						.setPreferenceInferrer(new AveragingPreferenceInferrer(model));
//
//				UserNeighborhood neighborhood = new NearestNUserNeighborhood(neighbors,
//						userCorrelation, model);
//				Recommender recommender = new GenericUserBasedRecommender(model,
//						neighborhood, userCorrelation);
//				Recommender cachingRecommender = new CachingRecommender(recommender);
//				return cachingRecommender;
//		    }
//		  };
//		Recommender recommender = builder.buildRecommender(myModel);
		List<RecommendedItem> recommendations = recommender.recommend("6", 5);
		for(RecommendedItem item : recommendations){
			System.out.println(item.getItem()+": "+ item.getValue());
		}
//		RecommenderEvaluator evaluator = new RMSRecommenderEvaluator();
//		double evaluation = evaluator.evaluate(builder, myModel, 0.9, 1.0);
//		System.out.println("evaluation = "+evaluation);
	}

}
