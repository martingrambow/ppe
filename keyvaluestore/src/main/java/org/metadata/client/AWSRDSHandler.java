package main.java.org.metadata.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;

/**
 * Handles interaction with DynamoDB.
 */
public class AWSRDSHandler {

	/**
	 * JDBC driver name.
	 */
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

	/**
	 * SQL-Db username.
	 */
	private static final String USER = "user";

	/**
	 * SQL password.
	 */
	private static final String PASS = "****";

	/**
	 * Name of database.
	 */
	private static final String DBNAME = "metainfo";

	/**
	 * SQL connection object.
	 */
	private Connection conn = null;

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AWSRDSHandler.class);

	/**
	 * Initializes Handler.
	 * 
	 * @param url
	 *            - url of database
	 */
	public AWSRDSHandler(final String url) {
		String dburl = "jdbc:mysql://" + url + "/" + DBNAME;
		LOGGER.debug("dbURL is " + dburl);
		try {
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(dburl, USER, PASS);
			LOGGER.info("Connection to database initalized.");
		} catch (SQLException e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		} catch (ClassNotFoundException e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Closes connection if there is one.
	 */
	public final void closeConnections() {
		if (conn != null) {
			try {
				if (!conn.isClosed()) {
					conn.close();
					LOGGER.info("Connection to database closed.");
				}
			} catch (SQLException e) {
				LOGGER.error(e.getLocalizedMessage(), e);
			}
		} else {
			LOGGER.warn("Unable to close connection, connection was null.");
		}
	}

	/**
	 * Creates PhotoData tables.
	 * 
	 * @param encrypted
	 *            - true, if the table should be created for encrypted contents
	 * @return true, if successful
	 */
	public boolean createTable(final boolean encrypted) {
		if (encrypted) {
			return runScript("createEnc.sql");
		} else {
			return runScript("createPlain.sql");
		}
	}

	/**
	 * Inserts a photo into table.
	 * 
	 * @param values
	 *            - value line to insert
	 * @param cipher
	 *            - cipher to use
	 * @return true, if successful
	 */
	public boolean insertPhotodata(final String[] values, final MyAESCipher cipher) {
		String command = "INSERT INTO Meta VALUES (";

		if (cipher == null) {
			// no encryption
			command = command + values[1] + ","; // ID
			command = command + values[2] + ","; // year
			command = command + values[3] + ","; // month
			command = command + values[4] + ","; // day
			command = command + values[5] + ","; // hour
			command = command + values[6] + ","; // minute
			command = command + values[7] + ","; // second
			command = command + "'" + values[8] + "');"; // location
			processSqlCommand(command);

			command = "INSERT INTO Tags VALUES ";
			for (int i = 9; i < values.length; i++) {
				command = command + "(" + values[1] + ","; // ID
				command = command + "'" + values[i] + "')"; // tag
				if (i < values.length - 1) {
					command = command + ",";
				}
			}
			processSqlCommand(command + ";");
		} else {
			// with encryption
			ArrayList<byte[]> binValues = new ArrayList<byte[]>();
			command = command + values[1] + ","; // ID
			for (int i = 2; i < 9; i++) {
				// year, month, day, hour, minute, second, location
				binValues.add(cipher.encrypt(values[i]));
				command = command + "?";
				if (i < 8) {
					command = command + ",";
				}
			}
			processSqlCommand(command + ");", null, binValues);

			command = "INSERT INTO Tags VALUES ";
			binValues = new ArrayList<byte[]>();
			for (int i = 9; i < values.length; i++) {
				command = command + "(" + values[1] + ","; // ID
				binValues.add(cipher.encrypt(values[i]));
				command = command + "?)"; // tag
				if (i < values.length - 1) {
					command = command + ",";
				}
			}
			processSqlCommand(command + ";", null, binValues);
		}
		return true;
	}

	/**
	 * Requests all Items which meet specified criteria.
	 * 
	 * @param query
	 *            - given query
	 * @return matching photo information
	 */
	public String[] queryData(final String query) {
		// year <= 2016&month >= 11
		String localQuery = query;
		if (query.contains("#")) {
			localQuery = query.substring(query.indexOf("#") + 1);
			LOGGER.info("query shortened");
		}

		String[] splitQuery = localQuery.split("[&|]");
		String[] conditions = new String[splitQuery.length];
		String condition = "";
		for (int i = 0; i < splitQuery.length; i++) {
			String[] parts = splitQuery[i].split(" ");
			String attrName = parts[0];
			String op = parts[1];
			String attrValue = parts[2];
			switch (attrName) {
			case "location":
				conditions[i] = "Meta.location = '" + attrValue + "'";
				break;
			case "tag":
				conditions[i] = "Tags.tag = '" + attrValue + "'";
				break;
			default:
				conditions[i] = "Meta." + attrName + " " + op + " " + attrValue;
				break;
			}
			if (i == 0) {
				condition = conditions[0];
			} else {
				if (localQuery.contains("&")) {
					condition = condition + " AND " + conditions[i];
				} else {
					condition = condition + " OR " + conditions[i];
				}
			}

		}

		String command = "";
		if (condition.contains("Tags.tag")) {
			// Join
			command = "SELECT DISTINCT Meta.photoID FROM Meta INNER JOIN Tags ON Meta.photoID=Tags.photoID";
		} else {
			// No join
			command = "SELECT Meta.photoID FROM Meta";
		}

		command = command + " WHERE " + condition + ";";
		return processSqlCommand(command);
	}

	/**
	 * Gets an encryted Item from DynamoDB.
	 * 
	 * @param cipher
	 *            - cipher use to decrypt
	 * @param photoIDs
	 *            - Set of photo IDs
	 * @return decrpyted item
	 */
	public String[] getItems(final MyAESCipher cipher, final ArrayList<Integer> photoIDs) {

		String[] resultArray = new String[photoIDs.size()];
		if (photoIDs.size() == 0) {
			return resultArray;
		}

		String command = "SELECT m.*, Group_Concat(t.tag) FROM Meta m INNER JOIN Tags t ON m.photoID=t.photoID WHERE";

		String condition = " m.photoid = " + photoIDs.get(0);
		for (int i = 0; i < photoIDs.size(); i++) {
			condition = condition + " OR m.photoID = " + photoIDs.get(i);
		}
		command = command + condition + " GROUP BY m.photoID";
		String[] results = processSqlCommand(command + ";", cipher, null);
		return results;
	}

	/**
	 * Runs given sql script.
	 * 
	 * @param name
	 *            - name of sql script in resource folder
	 * @return - false, if an error occurs
	 */
	private boolean runScript(final String name) {
		File sql = new File("resources/" + name);
		if (!sql.exists()) {
			LOGGER.error("Unable to find " + sql.getAbsolutePath());
			return false;
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(sql));
			String line = "";
			String cmd = "";
			while ((line = reader.readLine()) != null) {
				cmd = cmd + line;
				if (cmd.endsWith(";")) {
					processSqlCommand(cmd);
					cmd = "";
				}
			}
			reader.close();
		} catch (IOException e) {
			LOGGER.error(e.getLocalizedMessage(), e);
			return false;
		}
		return true;
	}

	/**
	 * Sends given command to sql server.
	 * 
	 * @param command
	 *            - command which is send to server
	 * @return Answer vom db server as array
	 */
	private String[] processSqlCommand(final String command) {
		return processSqlCommand(command, null, null);
	}

	/**
	 * Sends given command to sql server.
	 * 
	 * @param command
	 *            - command which is send to server
	 *            @param cipher - used cipher
	 *            @param contents - contents of result
	 * @return Answer vom db server as array
	 */
	private String[] processSqlCommand(final String command, final MyAESCipher cipher,
			final ArrayList<byte[]> contents) {
		ArrayList<String> resultlines = new ArrayList<String>();
		if (conn != null) {
			try {
				if (!conn.isClosed()) {
					PreparedStatement stmt = conn.prepareStatement(command);

					if (contents != null) {
						for (int i = 1; i <= contents.size(); i++) {
							stmt.setBytes(i, contents.get(i - 1));
						}
					}

					boolean resultsAvailable = false;
					try {
						resultsAvailable = stmt.execute();
					} catch (MySQLSyntaxErrorException e) {
						LOGGER.error("WRONG SYNTAX: " + command, e);
						return new String[0];
					}
					if (resultsAvailable) {
						ResultSet results = stmt.getResultSet();
						ResultSetMetaData meta = results.getMetaData();
						int columns = meta.getColumnCount();
						if (results != null) {
							if (results.isBeforeFirst()) {
								String resultLine = "";
								while (results.next()) {
									for (int i = 1; i < columns + 1; i++) {
										switch (meta.getColumnType(i)) {
										case Types.VARCHAR:
											// String
											resultLine = resultLine + "'" + results.getString(i) + "'###";
											break;
										case Types.INTEGER:
											// int
											resultLine = resultLine + results.getInt(i) + "###";
											break;
										case Types.VARBINARY:
											byte[] bytes = results.getBytes(i);
											String decrypted = "unknown";
											if (bytes.length % 16 == 0) {
												decrypted = cipher.decrypt(bytes);
											}
											resultLine = resultLine + decrypted + "###";
											break;
										case Types.LONGVARBINARY:
											byte[] blobBytes = results.getBytes(i);
											byte[] curr = new byte[16];
											int pos = 0;
											for (int j = 0; j < blobBytes.length; j++) {
												if (pos < 16) {
													curr[pos] = blobBytes[j];
												}
												if (pos == 15) {
													String plain = cipher.decrypt(curr);
													if (plain != null) {
														resultLine = resultLine + plain + "###";
													}
												}
												++pos;
												if (pos == 17) {
													pos = 0;
													curr = new byte[16];
												}
											}
											break;
										default:
											LOGGER.warn("No method for datatype " + meta.getColumnType(i) + ","
													+ meta.getColumnTypeName(i));
											break;
										}
									}
									if (resultLine.lastIndexOf("###") > 0) {
										resultLine = resultLine.substring(0, resultLine.lastIndexOf("###"));
									}
									resultlines.add(resultLine);
									resultLine = "";
								}
							}
						}
					}
					stmt.close();
				} else {
					LOGGER.error("Connection is closed.");
				}
			} catch (SQLException e) {
				LOGGER.error(e.getLocalizedMessage(), e);
			}
		} else {
			LOGGER.error("Unable to process SQL command, connection was null.");
		}
		String[] resultsAsStringArray = new String[resultlines.size()];
		for (int i = 0; i < resultlines.size(); i++) {
			resultsAsStringArray[i] = resultlines.get(i);
		}
		return resultsAsStringArray;
	}

}
