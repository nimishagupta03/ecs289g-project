/**
 * 
 */
package edu.ucdavis.cs.movieminer.taste;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Input: CSV file containing a list of customer movie ratings.
 * Ouput: Set of files, where each file is a movie and a set of ratings.
 * 
 * 
 * NOTE: By default the application will create all the files in the
 * current working directory.
 * 
 * @author jbeck
 *
 */
public class DataFormatter {

	private class DataRow{
		
		private Integer userId;
		private Integer movieId;
		private Integer rating;
		private String date;
		
		public DataRow(int userId, int movieId, int rating, String date){
			this.userId = userId;
			this.movieId = movieId;
			this.rating = rating;
			this.date = date;
		}

		public Integer getUserId() {
			return userId;
		}

		public Integer getMovieId() {
			return movieId;
		}

		public Integer getRating() {
			return rating;
		}
		
		public String getDate(){
			return date;
		}
		
	}
	
	private static final String FILE_PREFIX = "mv_";
	private List<DataRow> ratings = new LinkedList<DataRow>();
	private File dataFile;
	
	/**
	 * CSV data file.
	 * 
	 * @param dataFile
	 */
	DataFormatter(File dataFile){
		this.dataFile = dataFile;
	}
	
	public void format() throws IOException{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile)));
		String line;
		// Parse file
		while ( (line = reader.readLine()) != null){
			int firstComma = line.indexOf(',');
			int movieId = Integer.valueOf(line.substring(0, firstComma));
			int secondComma = line.indexOf(',', firstComma+1);
			int userId = Integer.parseInt(line.substring(firstComma+1, secondComma));
			int thirdComma = line.indexOf(',', secondComma + 1);
			int rating = Integer.parseInt(line.substring(secondComma+1, thirdComma));
			String date = line.substring(thirdComma+1);
			ratings.add(new DataRow(userId, movieId, rating, date));
		}
		Comparator<DataRow> movieIdSort = new Comparator<DataRow>(){

			@Override
			public int compare(DataRow row1, DataRow row2) {
				return row1.getMovieId().compareTo(row2.getMovieId());
			}
			
		};
		// Sort by movieId
		Collections.sort(ratings, movieIdSort);
		// Create a file for each movieId
		int currentMovieId = -1;
		BufferedWriter currentMovieFile = null; 
		for(DataRow rating : ratings){
			if (rating.getMovieId() == currentMovieId){
				// Add an entry to the current movie file
				currentMovieFile.write(rating.getUserId()+","+rating.getRating()+","+rating.getDate());
				currentMovieFile.newLine();
			}else{
				// Close previous file
				if (currentMovieFile != null){
					currentMovieFile.flush();
					currentMovieFile.close();
				}
				// Create a new file and add an entry
				currentMovieFile = new BufferedWriter(new FileWriter(FILE_PREFIX+rating.getMovieId()));
				currentMovieFile.write(Integer.toString(rating.getMovieId())+" ");
				currentMovieFile.newLine();
				currentMovieFile.write(rating.getUserId()+","+rating.getRating()+","+rating.getDate());
				currentMovieFile.newLine();
				currentMovieId = rating.getMovieId();
			}
		}
		currentMovieFile.flush();
		currentMovieFile.close();
	}
	
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String dataFile = args[0];
		DataFormatter formatter = new DataFormatter(new File(dataFile));
		formatter.format();

	}

}
