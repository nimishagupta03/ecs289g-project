package edu.ucdavis.cs.movieminer.taste;

import org.apache.log4j.Logger;
import org.springframework.core.io.FileSystemResource;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.eval.RecommenderBuilder;
import com.planetj.taste.impl.correlation.AveragingPreferenceInferrer;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.CachingRecommender;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

import edu.ucdavis.cs.movieminer.taste.recommender.CompositeRecommender;
import edu.ucdavis.cs.movieminer.taste.recommender.EvaluatingRecommender;
import edu.ucdavis.cs.movieminer.taste.recommender.LoggingRecommender;
import edu.ucdavis.cs.movieminer.taste.recommender.NoSuchElementRecommender;
import edu.ucdavis.cs.movieminer.taste.recommender.RandomRecommender;
import edu.ucdavis.cs.movieminer.taste.recommender.WeightedAverageRecommender;

public class SpelunkerRecommenderBuilder implements RecommenderBuilder {

	private static final Logger logger = Logger.getLogger(SpelunkerRecommenderBuilder.class);
	private int userNeighbors;
	EvaluatingRecommender evalRecommender;
	
	public void setUserNeighbors(int userNeighbors) {
		this.userNeighbors = userNeighbors;
	}

	@Override
	public Recommender buildRecommender(DataModel model)
			throws TasteException {
		// --- User-based recommender --
    	// build and return the Recommender to evaluate here
		UserCorrelation userCorrelation = new PearsonCorrelation(model);
		// Optional:
		userCorrelation
				.setPreferenceInferrer(new AveragingPreferenceInferrer(model));

		UserNeighborhood neighborhood = new NearestNUserNeighborhood(userNeighbors,
				userCorrelation, model);

		Recommender userRecommender = new GenericUserBasedRecommender(model,
				neighborhood, userCorrelation);
		// -- end User-based Recommender
		
		// -- SlopeOneRecommender
		// Make a weighted slope one recommender
//		Recommender slopeOneRecommender = new SlopeOneRecommender(model);
		// -- end SlopeOneRecommender
		
		// -- Item-based recommender
		KnnItemBasedRecommender itemBasedRecommender = 
			new KnnItemBasedRecommender(
				model,
				new FileSystemResource("/Users/jbeck/simScore17K-150each.ser"));
		// -- end Item-based recommender
		
		Recommender compositeRecommender = 
					new CompositeRecommender(model, 
									userRecommender,
//									slopeOneRecommender,
									itemBasedRecommender
									).
								setWeights(
									0.40d, 
//									0.10d,
									0.60d
									);
		logger.info("composed userRecommender, itemBasedRecommender");
		Recommender decoratedRecommender = new CachingRecommender(new LoggingRecommender(new RandomRecommender(new NoSuchElementRecommender(new WeightedAverageRecommender(compositeRecommender,0.75,0.25)))));
		evalRecommender = new EvaluatingRecommender(decoratedRecommender);
		return evalRecommender;
	}

	public EvaluatingRecommender getEvalRecommender() {
		return evalRecommender;
	}

}
