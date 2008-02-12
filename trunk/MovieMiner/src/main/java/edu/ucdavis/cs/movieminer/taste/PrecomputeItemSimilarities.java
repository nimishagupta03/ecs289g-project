/**
 * 
 */
package edu.ucdavis.cs.movieminer.taste;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.ItemCorrelation;
import com.planetj.taste.impl.common.IteratorUtils;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;

/**
 * 
 * @author pfishero
 * @version $Id$
 */
public class PrecomputeItemSimilarities {
	public static final Logger logger = Logger.getLogger(PrecomputeItemSimilarities.class);
	
	private static final Map<Item, Map<Item, Double>> correlationMaps = new HashMap<Item, Map<Item, Double>>(1009);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			logger.info("Usage:");
			logger.info(" java TasteTest {netflix_data_dir} ");
			System.exit(-1);
		} else {
			logger.info("using command line args: "+StringUtils.join(args, ", "));
		}
		
		final String netflixDataDir = args[0];
		logger.info("netflixDataDir="+netflixDataDir);
		
		final DataModel dataModel = new ECS289GNetflixDataModel(new File(netflixDataDir));
		
		final ItemCorrelation otherCorrelation = new PearsonCorrelation(dataModel);
		
		final List<? extends Item> items = IteratorUtils.iterableToList(dataModel.getItems());
		final int size = items.size();
		for (int i = 0; i < size; i++) {
			logger.info("item "+i);
			final Item item1 = items.get(i);
			for (int j = i + 1; j < size; j++) {
				final Item item2 = items.get(j);
				final double correlation = otherCorrelation.itemCorrelation(item1, item2);
				Map<Item, Double> map = correlationMaps.get(item1);
				if (map == null) {
					map = new HashMap<Item, Double>(1009);
					correlationMaps.put(item1, map);
				}
				map.put(item2, correlation);
			}
		}
		logger.info("saving simliarity maps");
		ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(new File(netflixDataDir, "simMap")));
		objOut.writeObject(correlationMaps);
		objOut.close();
	}

}
