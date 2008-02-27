/**
 * 
 */
package edu.ucdavis.cs.movieminer.taste;

import static edu.ucdavis.cs.movieminer.taste.Rating.createRating;
import edu.ucdavis.cs.movieminer.taste.recommender.LoggingRecommender;
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
import java.util.NoSuchElementException;
import java.util.Random;

import org.apache.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
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
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

import edu.ucdavis.cs.movieminer.taste.recommender.CompositeRecommender;

/**
 * Predicts a single rating for the passed in data and
 * recommender.
 * 
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
	 * Predicts a rating using the recommender implementation.
	 * If the estimate returned is NaN the estimate is the average
	 * rating for the user.  If no values exist in the training
	 * set to predict the estimate, it is chosen at random.
	 * 
	 * @throws TasteException
	 */
	public void recommend() throws TasteException{
		logger.info("Recommending ratings.");
		int nanCount = 0;
		int noTrainDataCount = 0;
		for(Rating rating : data){
			try{
				double estimate = recommender.estimatePreference(rating.getUserId(), rating.getMovieId());
				if (!Double.isNaN(estimate)){
					BigDecimal bd = new BigDecimal(estimate);
					bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
					logger.info("Estimated value rounded to int: "+bd.intValue());
					rating.setRating(bd.intValue());
				}else{
					logger.warn("Value could not be estimated, returned NaN.");
					nanCount++;
					rating.setRating(score(recommender.getDataModel().getUser(rating.getUserId())));
				}
			}catch (NoSuchElementException e){
				logger.warn("No data exists in the training set for item: " +rating.getMovieId());
				noTrainDataCount++;
				rating.setRating(guess());
			}
		}
		logger.info("Total NaN's encountered "+nanCount);
		logger.info("Elements not found in the training set: "+noTrainDataCount);
	}
	
	/**
	 * Score by first trying to compute the average, else guess.
	 * 
	 * @param user
	 * @return
	 */
	private int score(User user){
		// first try calculating an average
		logger.info("Score using user average.");
		int score = average(user);
		// guess random
		if (score == 0){
			logger.info("Average equal to zero, guess a random score.");
			score = guess();
		}
		logger.info("Set score:"+score);
		return score;
	}
	
	private int guess(){
		Random random = new Random();
		int score = random.nextInt(6);
		if (score == 0){
			score++;
		} 
		return score;
	}
	
	/**
	 * Compute the average score.
	 * 
	 * @param user
	 * @return
	 */
	private int average(User user){
		int n = 0;
		int sum = 0;
		for (Preference preference :  user.getPreferences()){
			sum += preference.getValue();
			n++;
		}
		return sum / n;
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
		int start = Integer.parseInt(args[4]);
		int end = Integer.parseInt(args[5]);	
		ratings = ratings.subList(start,end+1);
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
 KnnItemBasedRecommender itemBasedRecommender =
                                        new KnnItemBasedRecommender(
                                                model,
                                                new FileSystemResource("/home/becker/dev/netflix_data/simScore17K-150each.ser"));

					// -- end Item-based recommender
					
					Recommender compositeRecommender = 
								new CompositeRecommender(model, 
												userRecommender,
												//slopeOneRecommender,
												itemBasedRecommender
												).
											setWeights(
												0.40d, 
												//0.10d,
												0.60d
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
