/**
 * 
 */
package edu.ucdavis.cs.movieminer.taste;

import java.io.File;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.ItemCorrelation;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.eval.RecommenderBuilder;
import com.planetj.taste.eval.RecommenderEvaluator;
import com.planetj.taste.impl.correlation.AveragingPreferenceInferrer;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.eval.RMSRecommenderEvaluator;
import com.planetj.taste.impl.model.netflix.NetflixDataModel;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.CachingRecommender;
import com.planetj.taste.impl.recommender.GenericItemBasedRecommender;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.impl.recommender.slopeone.SlopeOneRecommender;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

import edu.ucdavis.cs.movieminer.taste.recommender.CompositeRecommender;

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

//		Recommender recommender = new SlopeOneRecommender(myModel);
		//Recommender cachingRecommender = new CachingRecommender(recommender);
		
		
		
		RecommenderBuilder builder = new RecommenderBuilder() {
			 public Recommender buildRecommender(DataModel model) throws TasteException {
			    	// --- User-based recommender --
			    	// build and return the Recommender to evaluate here
					UserCorrelation userCorrelation = new PearsonCorrelation(model);
					// Optional:
					userCorrelation
							.setPreferenceInferrer(new AveragingPreferenceInferrer(model));

					UserNeighborhood neighborhood = new NearestNUserNeighborhood(neighbors,
							userCorrelation, model);

					Recommender userRecommender = new GenericUserBasedRecommender(model,
							neighborhood, userCorrelation);
					// -- end User-based Recommender
					
					// -- SlopeOneRecommender
					// Make a weighted slope one recommender
					Recommender slopeOneRecommender = new SlopeOneRecommender(model);
					// -- end SlopeOneRecommender
					
					// -- Item-based recommender
					Recommender itemBasedRecommender =
						  new GenericItemBasedRecommender(model, (ItemCorrelation)userCorrelation);
					// -- end Item-based recommender
					
					Recommender compositeRecommender = 
								new CompositeRecommender(model, 
												userRecommender,
												slopeOneRecommender,
												itemBasedRecommender
												).
											setWeights(
												0.30d, 
												0.10d,
												0.60d
												);
					Recommender cachingRecommender = new CachingRecommender(compositeRecommender);
					
					return cachingRecommender;
			 }
		  };
//		Recommender recommender = builder.buildRecommender(myModel);
//		List<RecommendedItem> recommendations = recommender.recommend("6", 5);
//		for(RecommendedItem item : recommendations){
//			System.out.println(item.getItem()+": "+ item.getValue());
//		}
		RecommenderEvaluator evaluator = new RMSRecommenderEvaluator();
		double evaluation = evaluator.evaluate(builder, myModel, 0.9, 1.0);
		System.out.println("evaluation = "+evaluation);
	}

}
