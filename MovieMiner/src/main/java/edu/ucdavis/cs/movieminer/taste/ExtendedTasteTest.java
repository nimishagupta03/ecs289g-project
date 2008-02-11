/**
 * 
 */
package edu.ucdavis.cs.movieminer.taste;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.eval.RecommenderBuilder;
import com.planetj.taste.eval.RecommenderEvaluator;
import com.planetj.taste.impl.correlation.AveragingPreferenceInferrer;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.eval.RMSRecommenderEvaluator;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.CachingRecommender;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.impl.recommender.slopeone.SlopeOneRecommender;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

import edu.ucdavis.cs.movieminer.taste.recommender.CompositeRecommender;

/**
 * 
 * @author pfishero
 * @version $Id$
 */
public class ExtendedTasteTest {
	public static final Logger logger = Logger.getLogger(ExtendedTasteTest.class);

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			logger.info("Usage:");
			logger.info(" java TasteTest {netflix_data_dir} {k_neighbor_value}");
			System.exit(-1);
		} else {
			logger.info("using command line args: "+StringUtils.join(args, ", "));
		}
		
		final String netflixDataDir = args[0];
		final int neighbors = Integer.parseInt(args[1]);
		logger.info("netflixDataDir="+netflixDataDir+" neighbors="+neighbors);
		
		final DataModel myModel = new ECS289GNetflixDataModel(new File(netflixDataDir));

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
				
				Recommender compositeRecommender = 
							new CompositeRecommender(model, 
											userRecommender,
											slopeOneRecommender
											).
										setWeights(
											0.75d, 
											0.25d
											);
				Recommender cachingRecommender = new CachingRecommender(compositeRecommender);
				
				return cachingRecommender;
		    }
		  };
		RecommenderEvaluator evaluator = new RMSRecommenderEvaluator();
		double evaluation = evaluator.evaluate(builder, myModel, 0.9, 1.0);
		logger.info("evaluation = "+evaluation);
	}

}
