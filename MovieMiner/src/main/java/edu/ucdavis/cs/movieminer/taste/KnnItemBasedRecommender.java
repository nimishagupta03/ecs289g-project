/**
 * 
 */
package edu.ucdavis.cs.movieminer.taste;

import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.ItemCorrelation;
import com.planetj.taste.impl.common.Pair;
import com.planetj.taste.impl.correlation.GenericItemCorrelation;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.recommender.GenericItemBasedRecommender;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.recommender.ItemBasedRecommender;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Rescorer;

/**
 * Decorates {@link GenericItemBasedRecommender} with limits on the number of
 * similar items that it retrieves when doing the preference estimation in 
 * {@link #estimatePreference(Object, Object)}.
 * 
 * @author pfishero
 * @version $Id$
 */
public class KnnItemBasedRecommender implements ItemBasedRecommender {
	private final GenericItemBasedRecommender wrappedRecommender;
	private final int neighborCount;
	private final ItemCorrelation correlation;
	
	public KnnItemBasedRecommender(GenericItemBasedRecommender recommender) throws TasteException {
		this(recommender, 
				20, // 20 is the default number of neighbors
				new GenericItemCorrelation(
						new PearsonCorrelation(recommender.getDataModel()), 
						recommender.getDataModel())
				); 
	}
	
	public KnnItemBasedRecommender(GenericItemBasedRecommender recommender, 
									int neighbors,
									ItemCorrelation correlation) {
		this.wrappedRecommender = recommender;
		this.neighborCount = neighbors;
		this.correlation = correlation;
	}
	
	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.ItemBasedRecommender#mostSimilarItems(java.util.List, int, com.planetj.taste.recommender.Rescorer)
	 */
	@Override
	public List<RecommendedItem> mostSimilarItems(List<Object> arg0, int arg1,
			Rescorer<Pair<Item, Item>> arg2) throws TasteException {
		return wrappedRecommender.mostSimilarItems(arg0, arg1, arg2);
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.ItemBasedRecommender#mostSimilarItems(java.util.List, int)
	 */
	@Override
	public List<RecommendedItem> mostSimilarItems(List<Object> arg0, int arg1)
			throws TasteException {
		return wrappedRecommender.mostSimilarItems(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.ItemBasedRecommender#mostSimilarItems(java.lang.Object, int, com.planetj.taste.recommender.Rescorer)
	 */
	@Override
	public List<RecommendedItem> mostSimilarItems(Object arg0, int arg1,
			Rescorer<Pair<Item, Item>> arg2) throws TasteException {
		return wrappedRecommender.mostSimilarItems(arg0, arg1, arg2);
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.ItemBasedRecommender#mostSimilarItems(java.lang.Object, int)
	 */
	@Override
	public List<RecommendedItem> mostSimilarItems(Object arg0, int arg1)
			throws TasteException {
		return wrappedRecommender.mostSimilarItems(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.ItemBasedRecommender#recommendedBecause(java.lang.Object, java.lang.Object, int)
	 */
	@Override
	public List<RecommendedItem> recommendedBecause(Object arg0, Object arg1,
			int arg2) throws TasteException {
		return wrappedRecommender.recommendedBecause(arg0, arg1, arg2);
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.Recommender#estimatePreference(java.lang.Object, java.lang.Object)
	 */
	@Override
	public double estimatePreference(final Object userID, final Object itemID) throws TasteException {
		final DataModel model = getDataModel();
		final User theUser = model.getUser(userID);
		final Preference actualPref = theUser.getPreferenceFor(itemID);
		if (actualPref != null) {
			return actualPref.getValue();
		}
		final Item item = model.getItem(itemID);
		return doEstimatePreference(theUser, item);
	}
	
	private double doEstimatePreference(final User theUser, final Item item) throws TasteException {
		double preference = 0.0;
		double totalCorrelation = 0.0;
		
		// item-based KNN:
		// 1. Find the K nearest neighbors to item
		// 2. Determine which of those K were also rated by theUser
		// 3. Weighted interpolation between those in-common neighbors items' ratings		
		Set<Item> itemNeighbors = Sets.newHashSet();
		// retain only the items that have also been rated by the user 
		Iterables.addAll(itemNeighbors, 
				Iterables.filter(
					Iterables.transform(this.mostSimilarItems(item, neighborCount), 
							new Function<RecommendedItem, Item>(){
								@Override
								public Item apply(RecommendedItem recItem) {
									return recItem.getItem();
								}
							}
					), 
				new Predicate<Item>() {
					@Override
					public boolean apply(Item itemIn) {
						boolean keep = true;
						
						Preference pref = theUser.getPreferenceFor(itemIn);
						if (pref == null) {
							keep = false;
						}						
						
						return keep;
					}
				}));
		
		for (final Item similarItem : itemNeighbors) {
			Preference pref = theUser.getPreferenceFor(similarItem);
			double theCorrelation = correlation.itemCorrelation(item, pref.getItem());
			if (!Double.isNaN(theCorrelation)) {
				// Why + 1.0? correlation ranges from -1.0 to 1.0, and we want to use it as a simple
				// weight. To avoid negative values, we add 1.0 to put it in
				// the [0.0,2.0] range which is reasonable for weights
				theCorrelation += 1.0;
				preference += theCorrelation * pref.getValue();
				totalCorrelation += theCorrelation;
			}
		}
		
		return totalCorrelation == 0.0 ? Double.NaN : preference / totalCorrelation;
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.Recommender#getDataModel()
	 */
	@Override
	public DataModel getDataModel() {
		return wrappedRecommender.getDataModel();
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.Recommender#recommend(java.lang.Object, int, com.planetj.taste.recommender.Rescorer)
	 */
	@Override
	public List<RecommendedItem> recommend(Object arg0, int arg1,
			Rescorer<Item> arg2) throws TasteException {
		return wrappedRecommender.recommend(arg0, arg1, arg2);
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.Recommender#recommend(java.lang.Object, int)
	 */
	@Override
	public List<RecommendedItem> recommend(Object arg0, int arg1)
			throws TasteException {
		return wrappedRecommender.recommend(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.Recommender#removePreference(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void removePreference(Object arg0, Object arg1)
			throws TasteException {
		wrappedRecommender.removePreference(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.recommender.Recommender#setPreference(java.lang.Object, java.lang.Object, double)
	 */
	@Override
	public void setPreference(Object arg0, Object arg1, double arg2)
			throws TasteException {
		wrappedRecommender.setPreference(arg0, arg1, arg2);
		
	}

	/* (non-Javadoc)
	 * @see com.planetj.taste.common.Refreshable#refresh()
	 */
	@Override
	public void refresh() {
		wrappedRecommender.refresh();
	}
}
