package main.java.org.metadata.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.TimeKeeper;

/**
 * Main class in our client application.
 */
public final class ClientApplication {
	
	/**
	 * Url of database.
	 */
	private static String url = "metainfo.ca0rzgud7sgh.eu-west-1.rds.amazonaws.com";

	/**
	 * Cipher used for encryption and decryption.
	 */
	private static MyAESCipher cipher;

	/**
	 * Folder with scripts.
	 */
	private static String resourcefolder = "resources";

	/**
	 * Startport of ClientServers.
	 */
	private static int clientServerStartPort;

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientApplication.class);

	/**
	 * Communicator to contact year server.
	 */
	private static POPECommunicator yearCommunicator;
	/**
	 * Communicator to contact month server.
	 */
	private static POPECommunicator monthCommunicator;
	/**
	 * Communicator to contact hour server.
	 */
	private static POPECommunicator hourCommunicator;
	/**
	 * Communicator to contact location server.
	 */
	private static POPECommunicator locationCommunicator;
	/**
	 * Communicator to contact tag server.
	 */
	private static POPECommunicator tagCommunicator;

	/**
	 * Number of total inserts.
	 */
	private static int inserts = 0;

	/**
	 * Number of total queries.
	 */
	private static int queries = 0;

	/**
	 * Number of requested items.
	 */
	private static int queryItems = 0;

	/**
	 * Measures time needed for inserts due to POPE.
	 */
	private static TimeKeeper insertTimePOPE = new TimeKeeper();

	/**
	 * Measures time needed for inserts due to RDS.
	 */
	private static TimeKeeper insertTimeRDS = new TimeKeeper();

	/**
	 * Measures Time needed for queries due to POPE.
	 */
	private static TimeKeeper queryTimePOPE = new TimeKeeper();

	/**
	 * Measures Time needed for queries due to RDS.
	 */
	private static TimeKeeper queryTimeRDS = new TimeKeeper();

	/**
	 * True, if POPE should be applied.
	 */
	private static boolean encryption = false;

	/**
	 * True, if RDS should be used in combination with POPE.
	 */
	private static boolean popeAndRDS = false;

	/**
	 * Handler for RDS.
	 */
	private static AWSRDSHandler rdsHandler;

	/**
	 * Private default constructor.
	 */
	private ClientApplication() {

	}

	/**
	 * Starts our client application.
	 * 
	 * @param args
	 *            [0] - oracleHost [1] - oracle Port [2] - password [3] -
	 *            client-helper start port [4] - POPE Servers host [5] - POPE
	 *            servers startport [6] - name of script [7] - true, if
	 *            encryption should be applied [8] - true, if RDS should be
	 *            used also
	 * 
	 **/
	public static void main(final String[] args) {
		LOGGER.info("Client starting...");

		String oracleHost = args[0];

		int oracleStartPort = Integer.parseInt(args[1]);
		String password = args[2];
		clientServerStartPort = Integer.parseInt(args[3]);
		String serverHost = args[4];
		int serverStartPort = Integer.parseInt(args[5]);
		String script = args[6] + ".txt";
		String encryptionString = args[7];
		if (Boolean.parseBoolean(encryptionString)) {
			encryption = true;
		}
		String popeRDSString = args[8];
		if (Boolean.parseBoolean(popeRDSString)) {
			popeAndRDS = true;
		}

		ArrayList<Oracle> oracles = new ArrayList<Oracle>();
		if (encryption) {
			// init ciphers
			cipher = new MyAESCipher(password);
			LOGGER.info("Ciphers initialized.");

			// Init Oracles
			oracles.add(new Oracle(oracleHost, oracleStartPort, cipher.getPassword(), "year"));
			oracles.add(new Oracle(oracleHost, oracleStartPort + 1, cipher.getPassword(), "month"));
			oracles.add(new Oracle(oracleHost, oracleStartPort + 2, cipher.getPassword(), "hour"));
			oracles.add(new Oracle(oracleHost, oracleStartPort + 3, cipher.getPassword(), "location"));
			oracles.add(new Oracle(oracleHost, oracleStartPort + 4, cipher.getPassword(), "tag"));
			LOGGER.info("Oracles initialized.");

			for (Oracle o : oracles) {
				o.startOracle();
			}
			LOGGER.info("Oracles started.");
		}

		// Start RDShandler and create table
		rdsHandler = new AWSRDSHandler(url);
		rdsHandler.createTable(encryption);

		ArrayList<POPEClientServer> clientServer = new ArrayList<POPEClientServer>();

		if (encryption) {
			try {
				int seconds = 20;
				while (seconds > 0) {
					Thread.sleep(1000);
					LOGGER.info(seconds + "s until POPE servers must be started.");
					seconds = seconds - 1;
				}
			} catch (InterruptedException e) {
				LOGGER.error("Interrupted.");
			}

			LOGGER.info("Start helper-services on client side...");
			clientServer.add(new POPEClientServer(serverHost, serverStartPort, clientServerStartPort, password));
			clientServer
					.add(new POPEClientServer(serverHost, serverStartPort + 1, clientServerStartPort + 1, password));
			clientServer
					.add(new POPEClientServer(serverHost, serverStartPort + 2, clientServerStartPort + 2, password));
			clientServer
					.add(new POPEClientServer(serverHost, serverStartPort + 3, clientServerStartPort + 3, password));
			clientServer
					.add(new POPEClientServer(serverHost, serverStartPort + 4, clientServerStartPort + 4, password));

			for (POPEClientServer s : clientServer) {
				s.startClientServer();
			}
			LOGGER.info("Helper-services on client side started.");
		}
		try {
			int seconds = 5;
			while (seconds > 0) {
				Thread.sleep(1000);
				LOGGER.info("script starts in " + seconds + "s.");
				seconds = seconds - 1;
			}
		} catch (InterruptedException e) {
			LOGGER.error("Interrupted.");
		}

		LOGGER.info("Start running script...");
		LOGGER.info("; Kind; Count; Items; POPE; RDS; Encryption; Decryption");

		// Read&Run script
		runScript(script, encryption);

		if (encryption) {
			for (POPEClientServer s : clientServer) {
				s.stopClientServer();
			}
			for (Oracle o : oracles) {
				o.stopOracle();
			}
		}
	}

	/**
	 * Runs given script with or without encryption.
	 * 
	 * @param name
	 *            - name of script
	 * @param encryption
	 *            - true, if AES should be used.
	 * 
	 */
	private static void runScript(final String name, final boolean encryption) {
		File scriptFolder = new File(resourcefolder + "/" + name);
		if (!scriptFolder.exists()) {
			LOGGER.error("Unable to find " + scriptFolder.getAbsolutePath());
			return;
		}
		try {
			if (encryption) {
				yearCommunicator = new POPECommunicator(clientServerStartPort);
				monthCommunicator = new POPECommunicator(clientServerStartPort + 1);
				hourCommunicator = new POPECommunicator(clientServerStartPort + 2);
				locationCommunicator = new POPECommunicator(clientServerStartPort + 3);
				tagCommunicator = new POPECommunicator(clientServerStartPort + 4);
			}

			BufferedReader reader = new BufferedReader(new FileReader(scriptFolder));
			String line = "";
			String[] answer = new String[0];
			while ((line = reader.readLine()) != null) {
				answer = new String[0];
				String[] cmd = line.split("#");
				switch (cmd[0]) {
				case "SELECT":
					handleSelect(cmd);
					break;
				case "INSERT":
					handleInsert(cmd);
					break;
				default:
					LOGGER.warn("No actions for operation " + cmd[0] + " defined.");
					break;
				}
			}
			reader.close();
			// print stats
			answer = new String[0];
			if (encryption) {
				answer = yearCommunicator.sendText("s");
				LOGGER.info("year: " + answer[0]);
				answer = monthCommunicator.sendText("s");
				LOGGER.info("month: " + answer[0]);
				answer = hourCommunicator.sendText("s");
				LOGGER.info("hour: " + answer[0]);
				answer = locationCommunicator.sendText("s");
				LOGGER.info("location: " + answer[0]);
				answer = tagCommunicator.sendText("s");
				LOGGER.info("tag: " + answer[0]);

				yearCommunicator.closeSocket();
				monthCommunicator.closeSocket();
				hourCommunicator.closeSocket();
				locationCommunicator.closeSocket();
				tagCommunicator.closeSocket();
			}

			// LOGGER.info(inserts + " inserts in " + insertTime.getDuration() +
			// "ms and " + queries + " queries in "
			// + queryTimeTotal.getDuration() + "ms.");
			LOGGER.info("POPE:" + queryTimePOPE.getDuration() + "ms");
			LOGGER.info("RDS:" + queryTimeRDS.getDuration() + "ms");
		} catch (IOException e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}

	}

	/**
	 * Handles insert query.
	 * 
	 * @param cmd
	 *            - insert query
	 */
	private static void handleInsert(final String[] cmd) {
		String[] answer = new String[0];
		if (encryption) {
			insertTimePOPE.start();
			answer = yearCommunicator.sendText("ii " + cmd[2] + " " + cmd[1]);
			handleAnswer(answer);
			answer = monthCommunicator.sendText("ii " + cmd[3] + " " + cmd[1]);
			handleAnswer(answer);
			answer = hourCommunicator.sendText("ii " + cmd[5] + " " + cmd[1]);
			handleAnswer(answer);
			answer = locationCommunicator.sendText("i " + cmd[8] + " " + cmd[1]);
			handleAnswer(answer);
			for (int i = 9; i < cmd.length; i++) {
				answer = tagCommunicator.sendText("i " + cmd[i] + " " + cmd[1]);
				handleAnswer(answer);
			}
			insertTimePOPE.stop();
		} else {
			insertTimeRDS.start();
			rdsHandler.insertPhotodata(cmd, cipher);
			insertTimeRDS.stop();
		}
		if (popeAndRDS) {
			insertTimeRDS.start();
			rdsHandler.insertPhotodata(cmd, cipher);
			insertTimeRDS.stop();
		}

		inserts++;
		if (inserts % 10 == 0) {
			if (cipher != null) {
				LOGGER.info("; I; " + inserts + "; " + inserts + "; " + insertTimePOPE.getDuration() + "; "
						+ insertTimeRDS.getDuration() + "; " + cipher.getEncryptionTime() + "; "
						+ cipher.getDecryptionTime());
			} else {
				LOGGER.info("; I; " + inserts + "; " + inserts + "; " + insertTimePOPE.getDuration() + "; "
						+ insertTimeRDS.getDuration() + "; " + 0 + "; " + 0);
			}
		}
	}

	/**
	 * Handles range query.
	 * 
	 * @param cmd
	 *            - range query
	 */
	private static void handleSelect(final String[] cmd) {
		String query = cmd[1];
		String[] result = new String[0];
		if (encryption) {
			queryTimePOPE.start();
			ArrayList<Integer> answerset = getAnswerSet(query);
			queryTimePOPE.stop();
			result = new String[answerset.size()];
			if (popeAndRDS) {
				queryTimeRDS.start();
				result = rdsHandler.getItems(cipher, answerset);
				queryTimeRDS.stop();
			}
		} else {
			// No encryption, contact RDS directly
			queryTimeRDS.start();
			result = rdsHandler.queryData(query);
			queryTimeRDS.stop();
		}
		queryItems = queryItems + result.length;
		queries++;
		if (queries % 5 == 0) {
			if (cipher != null) {
				LOGGER.info("; Q; " + queries + "; " + queryItems + "; " + queryTimePOPE.getDuration() + "; "
						+ queryTimeRDS.getDuration() + "; " + cipher.getEncryptionTime() + "; "
						+ cipher.getDecryptionTime());
			} else {
				LOGGER.info("; Q; " + queries + "; " + queryItems + "; " + queryTimePOPE.getDuration() + "; "
						+ queryTimeRDS.getDuration() + "; " + 0 + "; " + 0);
			}
		}
	}

	/**
	 * Processes selection of a number.
	 * 
	 * @param parts
	 *            - parts of query
	 * @param minValue
	 *            - min value
	 * @param maxValue
	 *            - max value
	 * @param communicator
	 *            - communicator to use
	 * @return answer from POPE server
	 */
	private static String[] selectNumber(final String[] parts, final int minValue, final int maxValue,
			final POPECommunicator communicator) {
		if (communicator == null) {
			LOGGER.warn("Communicator was null...");
			return new String[0];
		}
		int number = Integer.parseInt(parts[2]);
		switch (parts[1]) {
		case "=":
			return communicator.sendText("qi " + (number - 1) + " " + number);
		case ">=":
			return communicator.sendText("qi " + (number - 1) + " " + maxValue);
		case "<=":
			return communicator.sendText("qi " + minValue + " " + number);
		default:
			return new String[0];
		}
	}

	/**
	 * Processes Selection of String value.
	 * 
	 * @param parts
	 *            - parts of query
	 * @param communicator
	 *            - communicator to use
	 * @return answer from POPE server
	 */
	private static String[] selectString(final String[] parts, final POPECommunicator communicator) {
		if (communicator == null) {
			LOGGER.warn("Communicator was null...");
			return new String[0];
		}
		String tmp = parts[2];
		int no = tmp.charAt(tmp.length() - 1);
		char last = (char) (no - 1);
		tmp = tmp.substring(0, tmp.length() - 1) + last;
		return communicator.sendText("q " + tmp + " " + parts[2]);
	}

	/**
	 * Gets a set with all Photo IDs which match all conditions.
	 * 
	 * @param query
	 *            - given conditions
	 * @return Set with all matching photo IDs
	 */
	private static ArrayList<Integer> getAnswerSet(final String query) {
		String[] splitQuery = query.split("[&|]");
		ArrayList<Integer> firstSet = null;
		ArrayList<Integer> secondSet = null;
		String[] answer = new String[0];
		for (String s : splitQuery) {
			String[] parts = s.split(" ");
			switch (parts[0]) {
			case "year":
				answer = selectNumber(parts, 0, 3000, yearCommunicator);
				break;
			case "month":
				answer = selectNumber(parts, 0, 13, monthCommunicator);
				break;
			case "hour":
				answer = selectNumber(parts, 0, 25, hourCommunicator);
				break;
			case "location":
				answer = selectString(parts, locationCommunicator);
				break;
			case "tag":
				answer = selectString(parts, tagCommunicator);
				break;
			default:
				break;
			}

			if (firstSet == null) {
				firstSet = new ArrayList<Integer>();
				for (String a : answer) {
					if (!a.contains("result comprises 0 elements:")) {
						String[] answerParts = a.split("'");
						int key = Integer.parseInt(answerParts[3]);
						firstSet.add(key);
					}
				}
			} else {
				secondSet = new ArrayList<Integer>();
				for (String a : answer) {
					if (!a.contains("result comprises 0 elements:")) {
						String[] answerParts = a.split("'");
						int key = Integer.parseInt(answerParts[3]);
						secondSet.add(key);
					}
				}
			}
		}
		ArrayList<Integer> answerset = new ArrayList<Integer>();
		if (query.contains("&")) {
			// combine first and second set with AND
			for (Integer i : firstSet) {
				for (Integer j : secondSet) {
					if (i.intValue() == j.intValue()) {
						// value is in fist AND second set
						answerset.add(i);
						break;
					}
				}
			}
		}
		if (query.contains("|")) {
			// combine first and second set with OR
			for (Integer i : firstSet) {
				answerset.add(i);
			}
			for (Integer i : secondSet) {
				boolean isin = false;
				for (Integer j : answerset) {
					if (j.intValue() == i.intValue()) {
						isin = true;
						break;
					}
				}
				if (!isin) {
					answerset.add(i);
				}
			}
		}
		if (!query.contains("&") && !query.contains("|")) {
			// only one condition
			answerset = firstSet;
		}
		return answerset;
	}

	/**
	 * Handles anser from POPE server.
	 * 
	 * @param answer
	 *            - answer from POPE server
	 */
	private static void handleAnswer(final String[] answer) {
		if (answer == null) {
			return;
		}
		if (answer.length == 0) {
			return;
		}
		if (answer[0] == null) {
			return;
		}
		if (answer[0].endsWith("inserted.")) {
			return;
		} else {
			LOGGER.info(answer[0]);
		}
	}
}
