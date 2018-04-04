package main.java.org.craft.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.CryptoService;
import main.java.TimeKeeper;
import main.java.craftinterfaces.OPECryptoScheme;
import main.java.crypto.ope.BoldyrevaNetworkCall;

/**
 * Runs sql scripts, en-/decrypts data if necessary.
 */
public class SQLScriptrunner {

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(SQLScriptrunner.class);

	/**
	 * Folder with sql scripts.
	 */
	private String resourcefolder = "resources";

	/**
	 * Helping variable to count queries.
	 */
	private int count = 0;

	/**
	 * JDBC driver name.
	 */
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

	/**
	 * SQL-DB username.
	 */
	private static final String USER = "user";

	/**
	 * SQL password.
	 */
	private static final String PASS = "*****";

	/**
	 * SQL connection object.
	 */
	private Connection conn = null;

	/**
	 * Service for encrytion of values.
	 */
	private CryptoService encryptionService;
	/**
	 * Service for decrytion of values.
	 */
	private CryptoService decryptionService;
	/**
	 * Reference service.
	 */
	private CryptoService nocryptionService;

	/**
	 * Timekeeper for en-/decryption time.
	 */
	private static TimeKeeper cryptoTime = new TimeKeeper();

	/**
	 * Timekeeper for transportation time.
	 */
	private static TimeKeeper transportTime = new TimeKeeper();

	/**
	 * Creates Scriptrunner object.
	 * 
	 * @param url
	 *            - URL of SQL server
	 * @param dbname
	 *            - name of database
	 * @param ssl
	 *            - true, if ssl should be used
	 * @param encryptionPort
	 *            - Port on which the service for encryption is running
	 * @param decryptionPort
	 *            - Port on which the service for decrytion is running
	 * @param nocryptionPort
	 *            - Port on which the reference service is running
	 */
	public SQLScriptrunner(final String url, final String dbname, final boolean ssl, final int encryptionPort,
			final int decryptionPort, final int nocryptionPort) {
		String dburl = "jdbc:mysql://" + url + "/" + dbname;
		if (ssl) {
			//dburl = dburl + "?verifyServerCertificate=true" + "&amp;useSSL=true&amp;requireSSL=true";
			dburl = dburl + "?verifyServerCertificate=false&useSSL=true&requireSSL=true";
			System.setProperty("javax.net.ssl.keyStore", "keystore/clientstore");
			System.setProperty("javax.net.ssl.keyStorePassword", "*****");
			System.setProperty("javax.net.ssl.trustStore", "keystore/clientstore");
			System.setProperty("javax.net.ssl.trustStorePassword", "*****");
			LOGGER.info("SSL encryption enabled.");
		} else {
			LOGGER.info("no SSL active.");
		}
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
		encryptionService = new CryptoService(encryptionPort);
		decryptionService = new CryptoService(decryptionPort);
		nocryptionService = new CryptoService(nocryptionPort);
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

		encryptionService.closeSocket();
		decryptionService.closeSocket();
		nocryptionService.closeSocket();
	}

	/**
	 * Runs sql script which creates database.
	 */
	public final void runCreateDBScript() {
		runScript("createDB.sql", "create", "none");
	}

	/**
	 * Runs sql script which fills database.
	 * 
	 * @param encryption
	 *            - used encryption scheme
	 */
	public final void runFillDBScript(final String encryption) {
		runScript("fillDB.sql", "fill", encryption);
	}

	/**
	 * Runs sql script which selects data in database.
	 * 
	 * @param encryption
	 *            - used encryption scheme
	 */
	public final void runSelectDBScript(final String encryption) {
		runScript("selectDB.sql", "select", encryption);
	}

	/**
	 * Runs given sql script.
	 * 
	 * @param name
	 *            - name of sql script
	 * @param kind
	 *            - kind of sql script (create, fill, select)
	 * @param encryption
	 *            - kind of encryption (none, boldyreva, ...)
	 */
	private void runScript(final String name, final String kind, final String encryption) {
		File sql = new File(resourcefolder + "/" + name);
		if (!sql.exists()) {
			LOGGER.error("Unable to find " + sql.getAbsolutePath());
			return;
		}
		int countAnswers = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(sql));
			String line = "";
			String cmd = "";
			while ((line = reader.readLine()) != null) {
				cmd = cmd + line;
				if (cmd.endsWith(";")) {
					switch (kind) {
					case "create":
						processSqlCommand(cmd);
						break;
					case "fill":
						cryptoTime.start();
						cmd = encryptFillCommand(cmd, encryption);
						cryptoTime.stop();
						transportTime.start();
						processSqlCommand(cmd);
						transportTime.stop();
						break;
					case "select":
						int selectAnswers = 0;
						transportTime.start();
						String[] answers = processSqlCommand(cmd);
						transportTime.stop();
						cryptoTime.start();
						for (int i = 0; i < answers.length; i++) {
							String answer = answers[i];
							answers[i] = decryptSelectAnswer(answer, encryption);
							countAnswers++;
							selectAnswers++;
							if (selectAnswers % 100 == 0) {
								LOGGER.info("Decrypted " + selectAnswers + " answers in this selection.");
							}
							if (countAnswers % 100 == 0) {
								LOGGER.info("Decrypted " + countAnswers + " answers in total.");
							}
						}
						cryptoTime.stop();
						break;
					default:
						LOGGER.warn("Kind " + kind + " is not known/implemented.");
						break;
					}
					cmd = "";
				}
			}
			reader.close();
		} catch (IOException e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}
		// print stats

		LOGGER.info("Cryptotime: " + getPrintableValue(cryptoTime.getDuration()));
		LOGGER.info("Real cryptiotime: " + getPrintableValue(BoldyrevaNetworkCall.getRealCryptionTime()));
		LOGGER.info("Conversiontime: " + getPrintableValue(BoldyrevaNetworkCall.getConversionTime()));
		LOGGER.info("Compressiontime: " + getPrintableValue(BoldyrevaNetworkCall.getCompressionTime()));
		
		LOGGER.info("Transporttime: " + getPrintableValue(transportTime.getDuration()));
		LOGGER.info("Total:" + getPrintableValue(transportTime.getDuration() + cryptoTime.getDuration()));

		LOGGER.info("encryptions: " + encryptionService.getCalls());
		LOGGER.info("decryptions: " + decryptionService.getCalls());
		LOGGER.info("nocryptions: " + nocryptionService.getCalls());
	}

	/**
	 * Sends given command to sql server.
	 * 
	 * @param command
	 *            - command which is send to server
	 * @return Answer from DB server
	 */
	private String[] processSqlCommand(final String command) {
		ArrayList<String> resultlines = new ArrayList<String>();
		if (conn != null) {
			try {
				if (!conn.isClosed()) {
					Statement stmt = conn.createStatement();
					boolean resultsAvailable = stmt.execute(command);
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
										case Types.BIGINT:
											//long
											resultLine = resultLine + results.getLong(i) + "###";
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

		count++;
		if (count % 100 == 0) {
			LOGGER.info(count + " commands processed in " + (System.currentTimeMillis() - Main.getStarttime()) + "ms.");
		}
		String[] resultsAsStringArray = new String[resultlines.size()];
		for (int i = 0; i < resultlines.size(); i++) {
			resultsAsStringArray[i] = resultlines.get(i);
		}
		return resultsAsStringArray;
	}

	/**
	 * Encrypts given Insert command.
	 * 
	 * @param command
	 *            - insert command to encrypt
	 * @param encryption
	 *            - encryption scheme
	 * @return encrypted String
	 */
	private String encryptFillCommand(final String command, final String encryption) {
		OPECryptoScheme scheme = new BoldyrevaNetworkCall(encryptionService, decryptionService, nocryptionService);

		try {
			int pos1 = command.indexOf("(");
			int pos2 = command.indexOf(")");
			String pre = command.substring(0, pos1 + 1);
			String post = command.substring(pos2);
			String content = command.substring(pos1 + 1, pos2);

			String[] fields = content.split(",");

			String encContent = "";
			for (int i = 0; i < fields.length; i++) {
				String field = fields[i].trim();

				if (field.startsWith("'")) {
					// String encryption
					String plain = field;
					if (plain.equals("''")) {
						field = "''";
					} else {
						String encrypted = "";
						switch (encryption) {
						case "boldyreva_off":
							plain = field.substring(1, field.lastIndexOf("'"));
							field = "'" + scheme.nocryptString(plain) + "'";
							break;
						case "boldyreva_on":
							plain = field.substring(1, field.lastIndexOf("'"));
							encrypted = scheme.encryptString(plain);
							if (encrypted.contains("'")) {
								encrypted = encrypted.replace("'", "''");
							}
							if (encrypted.contains("\\")) {
								encrypted = encrypted.replace("\\", "\\\\");
							}
							field = "'" + encrypted + "'";
							break;
						default:
							break;
						}
					}
				} else {
					// Integer encryption
					long plain = 0;
					switch (encryption) {
					case "boldyreva_off":
						plain = Long.parseLong(field);
						field = "" + scheme.nocrypt(plain);
						break;
					case "boldyreva_on":
						plain = Long.parseLong(field);
						field = "" + scheme.encrypt(plain);
						break;
					default:
						break;
					}
				}
				// field is encrypted now
				encContent = encContent + field + ",";
			}

			// remove last comma ',', if there is one
			if (encContent.contains(",")) {
				encContent = encContent.substring(0, encContent.lastIndexOf(","));
			}
			return pre + encContent + post;
		} catch (

		StringIndexOutOfBoundsException e) {
			LOGGER.error("error while processing: " + command, e);
			return "";
		}
	}

	/**
	 * Decrypts a select answer from database server.
	 * 
	 * @param line
	 *            - line to decrypt
	 * @param encryption
	 *            - used encryption scheme for encryption
	 * @return decrypted answer
	 */
	private String decryptSelectAnswer(final String line, final String encryption) {
		OPECryptoScheme scheme = new BoldyrevaNetworkCall(encryptionService, decryptionService, nocryptionService);
		String[] fields = line.split("###");
		String decContent = "";
		for (int i = 0; i < fields.length; i++) {
			String field = fields[i].trim();

			if (field.startsWith("'")) {
				// String decryption
				String encrypted = field;
				if (encrypted.equals("''")) {
					field = "''";
				} else {
					switch (encryption) {
					case "boldyreva_off":
						encrypted = field.substring(1, field.lastIndexOf("'"));
						field = "'" + scheme.nocryptString(encrypted) + "'";
						break;
					case "boldyreva_on":
						encrypted = field.substring(1, field.lastIndexOf("'"));
						if (encrypted.contains("''")) {
							encrypted = encrypted.replace("''", "'");
						}
						if (encrypted.contains("\\\\")) {
							encrypted = encrypted.replace("\\\\", "\\");
						}
						field = "'" + scheme.decryptString(encrypted) + "'";
						break;
					default:
						break;
					}
				}
			} else {
				// Integer encryption
				long encrypted = 0;
				switch (encryption) {
				case "boldyreva_off":
					encrypted = Long.parseLong(field);
					field = "" + scheme.nocrypt(encrypted);
					break;
				case "boldyreva_on":
					encrypted = Long.parseLong(field);
					field = "" + scheme.decrypt(encrypted);
					break;
				default:
					break;
				}
			}
			// field is decrypted now
			decContent = decContent + field + ",";
		}

		// remove last comma ','
		if (decContent.contains(",")) {
			decContent = decContent.substring(0, decContent.lastIndexOf(","));
		}

		return decContent;
	}

	/**
	 * Converts given value to a readable time format.
	 * 
	 * @param duration
	 *            - duration in ms
	 * @return readable time format
	 */
	private String getPrintableValue(final long duration) {
		String r = duration + "ms";
		r = r + ", " + (duration / 1000) + "sek";
		r = r + ", " + (duration / 1000 / 60) + ":" + ((duration / 1000) % 60) + "min";

		return r;
	}
}
