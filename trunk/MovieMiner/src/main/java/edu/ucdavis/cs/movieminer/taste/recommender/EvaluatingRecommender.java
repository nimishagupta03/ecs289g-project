package edu.ucdavis.cs.movieminer.taste.recommender;

import java.math.BigDecimal;
import java.util.List;

import org.apache.log4j.Logger;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;
import com.planetj.taste.recommender.Rescorer;

import edu.ucdavis.cs.movieminer.taste.InitialResults;

public class EvaluatingRecommender extends RecommenderDecorator {

	private static final Logger logger = Logger.getLogger(LoggingRecommender.class);
	int estimateCount = 0;
	int correctCount;
	int incorrectCount;
	long loss;
	private InitialResults results;
	
	
	public EvaluatingRecommender(Recommender recommender) {
		super(recommender);
		results = new InitialResults();
		results.readAnswers();
	}

	@Override
	public double estimatePreference(Object userID, Object itemID)
			throws TasteException {
		double preference = decoratedRecommender.estimatePreference(userID, itemID);
		BigDecimal bd = new BigDecimal(preference); 
		int intValue = bd.setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
		int actualValue = results.getRatingFor(Integer.parseInt(itemID.toString()), Integer.parseInt(userID.toString()));

		estimateCount++;
		if (intValue == actualValue) {
			correctCount++;
			logger.info("[eval] Correct");
		} else {
			incorrectCount++;
			loss += (intValue-actualValue)*(intValue-actualValue);
			logger.info("[eval] Incorrect: Actual "+actualValue+", Predicted "+intValue+", Loss "+loss);
		}
		
		if (estimateCount%100 == 0)
			logger.info("Number of estimated values: "+estimateCount);
		return preference;
	}

	@Override
	public List<RecommendedItem> recommend(Object userID, int howMany,
			Rescorer<Item> rescorer) throws TasteException {
		return decoratedRecommender.recommend(userID, howMany, rescorer);
	}

	@Override
	public List<RecommendedItem> recommend(Object userID, int howMany)
			throws TasteException {
		return decoratedRecommender.recommend(userID, howMany);
	}

	@Override
	public DataModel getDataModel() {
		return decoratedRecommender.getDataModel();
	}

	@Override
	public void removePreference(Object userID, Object itemID)
			throws TasteException {
		decoratedRecommender.removePreference(userID, itemID);
	}

	@Override
	public void setPreference(Object userID, Object itemID, double value)
			throws TasteException {
		decoratedRecommender.setPreference(userID, itemID, value);
	}

	@Override
	public void refresh() {
		decoratedRecommender.refresh();
	}

	public int getEstimateCount() {
		return estimateCount;
	}

	public int getCorrectCount() {
		return correctCount;
	}

	public int getIncorrectCount() {
		return incorrectCount;
	}

	public long getLoss() {
		return loss;
	}

	public InitialResults getResults() {
		return results;
	}	
}
