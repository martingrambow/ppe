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
 * Generates INSERT SQL queries.
 */
public final class InsertGenerator {

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(InsertGenerator.class);

	/**
	 * Random number generator.
	 */
	private static Random r = new Random();

	/**
	 * List of material keys.
	 */
	private static ArrayList<String> materialKeys = new ArrayList<String>();

	/**
	 * List of customer keys.
	 */
	private static ArrayList<Integer> customerKeys = new ArrayList<Integer>();

	/**
	 * List of resource keys.
	 */
	private static ArrayList<Integer> resourceKeys = new ArrayList<Integer>();

	/**
	 * List of order keys.
	 */
	private static ArrayList<Integer> orderKeys = new ArrayList<Integer>();

	/**
	 * Writer used to write contents.
	 */
	private static BufferedWriter writer;

	/**
	 * Creates InsertGenerator object.
	 */
	private InsertGenerator() {
	}

	/**
	 * Gets a string value from parameter list.
	 * 
	 * @param possibleValues1
	 *            - list of possible values
	 * @param possibleValues2
	 *            - second list of possible values, can be null
	 * @param randomID
	 *            - appends a random ID to make value unique
	 * @return randomly selected string value
	 */
	private static String getStringValue(final String[] possibleValues1, final String[] possibleValues2,
			final boolean randomID) {
		int pos = r.nextInt(possibleValues1.length);
		String value = possibleValues1[pos];
		if (possibleValues2 != null) {
			pos = r.nextInt(possibleValues2.length);
			value = value + " " + possibleValues2[pos];
		}
		if (randomID) {
			value = value + " " + r.nextInt(10000);
		}
		return value;
	}

	/**
	 * Gets a integer value.
	 * 
	 * @param lowerBound
	 *            - lower bound of value
	 * @param upperBound
	 *            - upper bound of value
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
			File fillDB = new File("fillDB.sql");
			if (fillDB.exists()) {
				fillDB.delete();
			}
			fillDB.createNewFile();

			FileWriter fw = new FileWriter(fillDB);
			writer = new BufferedWriter(fw);

			printMaterials(2500);
			printCustomers(2500);
			printEmployees(1000);
			printMachines(1500);
			printOrders(10000);
			printOrdersMaterials(20000);
			printOrdersResources(10000);

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
	 * Prints insert commands for table materials.
	 * 
	 * @param count
	 *            - count of commands
	 * @throws IOException
	 *             - in case of errors
	 */
	private static void printMaterials(final int count) throws IOException {
		String tablename = "Materials";
		String[] names1 = new String[] { "tile", "glue", "cement", "gypsum", "powder", "screws" };
		String[] names2 = new String[] { "unglazed", "red", "green", "blue", "white", "ceramic", "standard", "small",
				"large", "special" };
		String[] comments = new String[] { "deliverable", "sold out", "earmarked", "in stock 1A", "in stock 25C",
				"in stock 4B", "in stock 3A", "in stock 9A" };

		int current = 0;
		while (current < count) {
			String key = getStringValue(names1, names2, true);
			boolean duplicate = false;
			for (String k : materialKeys) {
				if (k.equals(key)) {
					duplicate = true;
				}
			}
			if (!duplicate) {
				materialKeys.add(key);
				writeln("INSERT INTO " + tablename + " VALUES ('" + key + "', " + getIntegerValue(1, 100) + ", '"
						+ getStringValue(comments, null, false) + "');");
				current++;
			}
		}
	}

	/**
	 * Prints insert commands for table customers.
	 * 
	 * @param count
	 *            - count of commands
	 * @throws IOException
	 *             - in case of errors
	 */
	private static void printCustomers(final int count) throws IOException {
		String tablename = "Customers";
		String[] names1 = new String[] { "Jarrett", "Jerome", "Nickolas", "Cornelius", "Miles", "Cliff", "Duncan",
				"Jacinto", "Nannette", "Nereida", "Mercedes", "Migdalia", "Rebeca", "Ellie" };
		String[] names2 = new String[] { "Kozel", "Studdard", "Haase", "Palomo", "Owings", "Spatz", "Harvell" };
		String[] adress = new String[] { "William Street", "Devonshire Drive", "Holly Drive", "9th Street West",
				"Meadow Street", "Evergreen Drive", "Route 17", "Parker Street" };
		String[] zipcodes = new String[] { "11510 Baldwin NY", "13090 Liverpool NY", "60185 West Chicago IL",
				"96815 Honolulu HI" };
		String[] sections = new String[] { "north", "west", "premium", "south", "east", "promotion" };

		int current = 0;
		while (current < count) {
			int key = getIntegerValue(0, 10000);
			boolean duplicate = false;
			for (Integer k : customerKeys) {
				if (k.intValue() == key) {
					duplicate = true;
				}
			}
			if (!duplicate) {
				customerKeys.add(key);
				writeln("INSERT INTO " + tablename + " VALUES (" + key + ", '" + getStringValue(names1, names2, false)
						+ "', '" + getStringValue(adress, null, true) + "', '" + getStringValue(zipcodes, null, false)
						+ "', '" + getStringValue(sections, null, false) + "');");
				current++;
			}
		}
	}

	/**
	 * Prints insert commands for table Employees and resources.
	 * 
	 * @param count
	 *            - count of commands
	 * @throws IOException
	 *             - in case of errors
	 */
	private static void printEmployees(final int count) throws IOException {
		String tablename1 = "Resources";
		String tablename2 = "Employees";
		String[] names1 = new String[] { "Jarrett", "Jerome", "Nickolas", "Cornelius", "Miles", "Cliff", "Duncan",
				"Jacinto", "Nannette", "Nereida", "Mercedes", "Migdalia", "Rebeca", "Ellie" };
		String[] names2 = new String[] { "Kozel", "Studdard", "Haase", "Palomo", "Owings", "Spatz", "Harvell" };
		String[] adress = new String[] { "William Street", "Devonshire Drive", "Holly Drive", "9th Street West",
				"Meadow Street", "Evergreen Drive", "Route 17", "Parker Street" };
		String[] zipcodes = new String[] { "11510 Baldwin NY", "13090 Liverpool NY", "60185 West Chicago IL",
				"96815 Honolulu HI" };

		int current = 0;
		while (current < count) {
			int key = getIntegerValue(0, 10000);
			boolean duplicate = false;
			for (Integer k : resourceKeys) {
				if (k.intValue() == key) {
					duplicate = true;
				}
			}
			if (!duplicate) {
				resourceKeys.add(key);
				writeln("INSERT INTO " + tablename1 + " VALUES (" + key + "," + getIntegerValue(10, 50) + ");");

				writeln("INSERT INTO " + tablename2 + " VALUES (" + key + ", '" + getStringValue(names1, names2, false)
						+ "', '" + getStringValue(adress, null, true) + "', '" + getStringValue(zipcodes, null, false)
						+ "', '" + getIntegerValue(20000000, 900000000) + "');");
				current++;
			}
		}
	}

	/**
	 * Prints insert commands for table machines and resources.
	 * 
	 * @param count
	 *            - count of commands
	 * @throws IOException
	 *             - in case of errors
	 */
	private static void printMachines(final int count) throws IOException {
		String tablename1 = "Resources";
		String tablename2 = "Machines";
		String[] labels = new String[] { "mixer", "saw large", "saw small", "molding cutter", "grinding machine" };

		int current = 0;
		while (current < count) {
			int key = getIntegerValue(0, 10000);
			boolean duplicate = false;
			for (Integer k : resourceKeys) {
				if (k.intValue() == key) {
					duplicate = true;
				}
			}
			if (!duplicate) {
				resourceKeys.add(key);
				writeln("INSERT INTO " + tablename1 + " VALUES (" + key + "," + getIntegerValue(5, 30) + ");");

				writeln("INSERT INTO " + tablename2 + " VALUES (" + key + ", '" + getStringValue(labels, null, true)
						+ "', " + getIntegerValue(2000, 2017) + ");");
				current++;
			}
		}
	}

	/**
	 * Prints insert commands for table orders.
	 * 
	 * @param count
	 *            - count of commands
	 * @throws IOException
	 *             - in case of errors
	 */
	private static void printOrders(final int count) throws IOException {
		String tablename = "Orders";
		String[] days = new String[] { "03", "09", "15", "18", "22", "28", "11" };
		String[] months = new String[] { "01", "03", "05", "08", "10", "11", "12" };
		String[] years = new String[] { "2001", "2003", "2005", "2008", "2010", "2011", "2012", "2014", "2015",
				"2016" };

		int current = 0;
		while (current < count) {
			int key = getIntegerValue(0, 10000);
			boolean duplicate = false;
			for (Integer k : orderKeys) {
				if (k.intValue() == key) {
					duplicate = true;
				}
			}
			if (!duplicate) {
				orderKeys.add(key);
				String date = getStringValue(years, null, false) + "." + getStringValue(months, null, false) + "."
						+ getStringValue(days, null, false);
				int customerID = customerKeys.get(r.nextInt(customerKeys.size())).intValue();
				writeln("INSERT INTO " + tablename + " VALUES (" + key + ", '" + date + "', " + customerID + ");");
				current++;
			}
		}
	}

	/**
	 * Prints insert commands for table orders_materials.
	 * 
	 * @param count
	 *            - count of commands
	 * @throws IOException
	 *             - in case of errors
	 */
	private static void printOrdersMaterials(final int count) throws IOException {
		String tablename = "Orders_Materials";

		ArrayList<String> primKeyList = new ArrayList<String>();

		int current = 0;
		while (current < count) {
			String materialName = materialKeys.get(r.nextInt(materialKeys.size()));
			int orderID = orderKeys.get(r.nextInt(orderKeys.size())).intValue();

			boolean duplicate = false;
			String key = materialName + orderID;
			for (String k : primKeyList) {
				if (k.equals(key)) {
					duplicate = true;
				}
			}
			if (!duplicate) {
				primKeyList.add(key);
				writeln("INSERT INTO " + tablename + " VALUES ('" + materialName + "', " + orderID + ", "
						+ getIntegerValue(1, 300) + ");");
				current++;
			}
		}
	}

	/**
	 * Prints insert commands for table orders_resources.
	 * 
	 * @param count
	 *            - count of commands
	 * @throws IOException
	 *             - in case of errors
	 */
	private static void printOrdersResources(final int count) throws IOException {
		String tablename = "Orders_Resources";

		ArrayList<String> primKeyList = new ArrayList<String>();
		int current = 0;
		while (current < count) {
			int resourceID = resourceKeys.get(r.nextInt(resourceKeys.size())).intValue();
			int orderID = orderKeys.get(r.nextInt(orderKeys.size())).intValue();

			boolean duplicate = false;
			String key = "" + resourceID + orderID;
			for (String k : primKeyList) {
				if (k.equals(key)) {
					duplicate = true;
				}
			}
			if (!duplicate) {
				primKeyList.add(key);
				writeln("INSERT INTO " + tablename + " VALUES (" + resourceID + ", " + orderID + ", "
						+ getIntegerValue(1, 50) + ");");
				current++;
			}
		}
	}
}
