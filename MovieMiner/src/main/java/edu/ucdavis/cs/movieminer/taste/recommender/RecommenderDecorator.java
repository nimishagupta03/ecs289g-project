/**
 * 
 */
package edu.ucdavis.cs.movieminer.taste.recommender;

import java.util.List;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;
import com.planetj.taste.recommender.Rescorer;

/**
 * Standard decorator with default chaining implementation.
 * 
 * @author jbeck
 *
 */
public abstract class RecommenderDecorator implements Recommender {

	protected Recommender decoratedRecommender;
	
	public RecommenderDecorator(Recommender recommender){
		decoratedRecommender = recommender;
	}
}
