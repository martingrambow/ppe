package main.java;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates POPE Scripts.
 */
public final class POPEScriptGenerator {

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(POPEScriptGenerator.class);

	/**
	 * Random number generator.
	 */
	private static Random r = new Random();

	/**
	 * List of all photo IDs.
	 */
	private static ArrayList<Integer> photoIDs = new ArrayList<>();

	/**
	 * Array of tags.
	 */
	private static String[] tags = new String[] { "summer", "winter", "spring", "autumn", "winter", "nature", "family",
			"water", "sport", "work", "home", "car", "bike", "cat", "dog" };

	/**
	 * Array of locations.
	 */
	private static String[] locations = new String[] { "Amsterdam", "Ankara", "Athen", "Belgrad", "Berlin", "Bern",
			"Bratislava", "Brüssel", "Budapest", "Bukarest", "Dublin", "Helsinki", "Kiew", "Kopenhagen", "Lissabon",
			"Ljubljana", "London", "Luxemburg", "Madrid", "Minsk", "Monaco", "Moskau", "Nikosia", "Oslo", "Paris",
			"Podgorica", "Prag", "Reykjavik", "Riga", "Rom", "Sarajevo", "Skopje", "Sofia", "Stockholm", "Tallinn",
			"Tirana", "Vaduz", "Valletta", "Vilnius", "Warschau", "Wien", "Zagreb" };

	/**
	 * Writer used to write contents.
	 */
	private static BufferedWriter writer;

	/**
	 * Creates InsertGenerator object.
	 */
	private POPEScriptGenerator() {
	}

	/**
	 * Gets a string value from parameter list.
	 * 
	 * @param possibleValues1
	 *            - list of possible values
	 * @return randomly selected string value
	 */
	private static String getStringValue(final String[] possibleValues1) {
		int pos = r.nextInt(possibleValues1.length);
		String value = possibleValues1[pos];
		return value;
	}

	/**
	 * Gets a integer value.
	 * 
	 * @param lowerBound
	 *            - lower bound of value (incl)
	 * @param upperBound
	 *            - upper bound of value (excl)
	 * @return value between bounds
	 */
	private static int getIntegerValue(final int lowerBound, final int upperBound) {
		int value = r.nextInt(upperBound - lowerBound);
		return value + lowerBound;
	}

	/**
	 * Main method.
	 * 
	 * @param args
	 *            - not used
	 */
	public static void main(final String[] args) {

		try {
			File script = new File("E.txt");
			if (script.exists()) {
				script.delete();
			}
			script.createNewFile();

			FileWriter fw = new FileWriter(script);
			writer = new BufferedWriter(fw);

			for (int i = 0; i < 100; i++) {
				printInsertsAndSelections(100, 5);
				printInsertsAndFixSelections(100, 5);
			}

			writer.flush();
			writer.close();
		} catch (IOException e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Writes a given line with writer.
	 * 
	 * @param line
	 *            - line to be written
	 * @throws IOException
	 *             - in case of errors
	 */
	private static void writeln(final String line) throws IOException {
		writer.write(line + "\n");
	}

	/**
	 * Prints x inserts followed by y selections.
	 * 
	 * @param inserts
	 *            - number of inserts
	 * @param selections
	 *            - number of selections
	 * @throws IOException
	 *             - In case of errors
	 */
	private static void printInsertsAndSelections(final int inserts, final int selections) throws IOException {
		printInserts(inserts);
		printSelectionQueries(2, selections);
	}

	/**
	 * Prints x inserts followed by y fixed selections.
	 * 
	 * @param inserts
	 *            - number of inserts
	 * @param selections
	 *            - number of fixed selections
	 * @throws IOException
	 *             - In case of errors
	 */
	private static void printInsertsAndFixSelections(final int inserts, final int selections) throws IOException {
		printInserts(inserts);

		int count = selections;
		String line = "";
		if (count > 0) {
			line = "SELECT#location = Prag&year = 2014";
			writeln(line);
			count--;
		}

		if (count > 0) {
			line = "SELECT#tag = autumn&location = Dublin";
			writeln(line);
			count--;
		}

		if (count > 0) {
			line = "SELECT#month >= 8|location = Kiew";
			writeln(line);
			count--;
		}

		if (count > 0) {
			line = "SELECT#hour <= 6&year >= 2015";
			writeln(line);
			count--;
		}

		if (count > 0) {
			line = "SELECT#hour = 11|tag = spring";
			writeln(line);
			count--;
		}
	}

	/**
	 * Prints a given number of insert commands.
	 * 
	 * @param count
	 *            - given number
	 * @throws IOException
	 *             - In case of errors
	 */
	private static void printInserts(final int count) throws IOException {
		int current = 0;
		while (current < count) {
			int key = getIntegerValue(0, 50000);
			boolean duplicate = false;
			for (int k : photoIDs) {
				if (k == key) {
					duplicate = true;
				}
			}
			if (!duplicate) {
				photoIDs.add(key);

				int year = getIntegerValue(1990, 2017);
				int month = getIntegerValue(1, 13);
				int day = getIntegerValue(1, 29);
				int hour = getIntegerValue(0, 24);
				int minute = getIntegerValue(0, 60);
				int sec = getIntegerValue(0, 60);

				String line = "INSERT#" + key;
				line = line + "#" + year + "#" + month + "#" + day;
				line = line + "#" + hour + "#" + minute + "#" + sec;

				String location = getStringValue(locations);
				line = line + "#" + location;

				int tagnr = getIntegerValue(1, 10);
				ArrayList<String> taglist = new ArrayList<String>();
				while (tagnr > 0) {
					boolean duplicate2 = false;
					String t = getStringValue(tags);
					for (String s : taglist) {
						if (s.equals(t)) {
							duplicate2 = true;
						}
					}
					if (!duplicate2) {
						taglist.add(t);
						line = line + "#" + t;
						tagnr = tagnr - 1;
					}
				}

				writeln(line);
				current++;
			}
		}
	}

	/**
	 * Prints a given number of selection queries.
	 * 
	 * @param maxConditions
	 *            - maximum numbers of conditions
	 * @param count
	 *            - given number
	 * @throws IOException
	 *             - In case of errors
	 */
	private static void printSelectionQueries(final int maxConditions, final int count) throws IOException {
		int current = 0;
		while (current < count) {
			printSelectionQuery(1 + r.nextInt(maxConditions));
			current++;
		}
	}

	/**
	 * Prints a random selection query with a given number of conditions.
	 * 
	 * @param conditions
	 *            - number of conditions
	 * @throws IOException
	 *             - In case of errors
	 */
	private static void printSelectionQuery(final int conditions) throws IOException {
		String line = "SELECT#";

		String[] comparisions1 = new String[] { ">=", "<=", "=" };

		String[] operators = new String[] { "&", "|" };

		String[] classifiers = new String[] { "year", "month", "hour", "tag", "location" };

		int conds = conditions;

		ArrayList<String> classifierlist = new ArrayList<String>();
		while (conds > 0) {

			boolean duplicate = false;
			String c = getStringValue(classifiers);
			for (String d : classifierlist) {
				if (d.equals(c)) {
					duplicate = true;
				}
			}
			if (!duplicate) {
				line = line + c;
				classifierlist.add(c);
				if (c.equals("tag")) {
					line = line + " = " + getStringValue(tags);
				}
				if (c.equals("location")) {
					line = line + " = " + getStringValue(locations);
				}
				if (c.equals("year")) {
					line = line + " " + getStringValue(comparisions1) + " " + getIntegerValue(1990, 2017);
				}
				if (c.equals("month")) {
					line = line + " " + getStringValue(comparisions1) + " " + getIntegerValue(1, 13);
				}
				if (c.equals("hour")) {
					line = line + " " + getStringValue(comparisions1) + " " + getIntegerValue(0, 24);
				}
				if (conds > 1) {
					line = line + getStringValue(operators);
				}
				conds = conds - 1;
			}
		}
		writeln(line);
	}
}
