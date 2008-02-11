/**
 * 
 */
package edu.ucdavis.cs.movieminer.taste;

import static edu.ucdavis.cs.movieminer.taste.Rating.createRating;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.ItemCorrelation;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.eval.RecommenderBuilder;
import com.planetj.taste.impl.correlation.AveragingPreferenceInferrer;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.model.netflix.NetflixDataModel;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.CachingRecommender;
import com.planetj.taste.impl.recommender.GenericItemBasedRecommender;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

import edu.ucdavis.cs.movieminer.taste.recommender.CompositeRecommender;
import edu.ucdavis.cs.movieminer.taste.recommender.LoggingRecommender;

/**
 * Predicts a single rating for the passed in data and
 * recommender.
 * 
 * NOTE: Rounds the estimated value to an integer using BigDecimal.ROUND_HALF_UP
 * 
 * @author jbeck
 *
 */
public class MovieMiner {

	private static final Logger logger = Logger.getLogger(MovieMiner.class);
	private Collection<Rating> data;
	private Recommender recommender;
	
	public MovieMiner(Collection<Rating> data, Recommender recommender){
		this.data = data;
		this.recommender = recommender;
	}
	
	public Collection<Rating> getData(){
		return data;
	}
	
	/**
	 * NOTE: Rounds the estimated value using BigDecimal.ROUND_HALF_UP
	 * 
	 * @throws TasteException
	 */
	public void recommend() throws TasteException{
		logger.info("Recommending ratings.");
		for(Rating rating : data){
			double estimate = recommender.estimatePreference(rating.getUserId(), rating.getMovieId());
			if (!Double.isNaN(estimate)){
				BigDecimal bd = new BigDecimal(estimate);
				bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
				logger.info("Estimated value rounded to int: "+bd.intValue());
				rating.setRating(bd.intValue());
			}else{
				logger.warn("Value could not be estimated, returned NaN.");
			}
		}
	}
	
	/**
	 * Writes ratings to writer.
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void write(Writer writer) throws IOException{
		logger.info("Writing ratings to file.");
		Rating.write(data, writer);
	}
	
	/**
	 * args[0] is the test file.
	 * args[1] is the netflix home directory, containing training_set directory and movie_titles.txt
	 * args[2] number of neighbors used in prediction
	 * args[3] output file where predicted ratings are stored
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws TasteException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void main(String args[]) throws IOException, ClassNotFoundException, TasteException, InstantiationException, IllegalAccessException{
		if (args.length < 4) {
			System.out.println("Usage:");
			System.out.println(" java MovieMiner {netflix_test_file} {netflix_home_dir} {k_neighbor_value} {output file}");
			System.exit(-1);
		} else {
			System.out.println("using command line args: "+args);
		}
		final int neighbors = Integer.parseInt(args[2]);
		// Read in the ratings from the data set.
		List<Rating> ratings = new LinkedList<Rating>(); 
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
		String line;
		// Parse file
		while ( (line = reader.readLine()) != null){
			ratings.add(createRating(line));
		}
		// Build data model
		DataModel myModel = new NetflixDataModel(new File(args[1]));
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
					//Recommender slopeOneRecommender = new SlopeOneRecommender(model);
					// -- end SlopeOneRecommender
					
					// -- Item-based recommender
					Recommender itemBasedRecommender =
						  new GenericItemBasedRecommender(model, (ItemCorrelation)userCorrelation);
					// -- end Item-based recommender
					
					Recommender compositeRecommender = 
								new CompositeRecommender(model, 
												userRecommender,
												//slopeOneRecommender,
												itemBasedRecommender
												).
											setWeights(
												0.30d, 
												//0.10d,
												0.70d
												);
					Recommender cachingRecommender = new CachingRecommender(compositeRecommender);
					
					return cachingRecommender;
			 }
		  };
		Recommender recommender = builder.buildRecommender(myModel);
		// Decorate with a logger to see whats going on.
		Recommender decoratedRecommender = new LoggingRecommender(recommender);
		MovieMiner miner = new MovieMiner(ratings, decoratedRecommender);
		// Output the recommendation to a file.
		miner.recommend();
		Writer writer = new BufferedWriter(new FileWriter(args[3]));
		miner.write(writer);
		writer.flush();
		writer.close();
	}
}
