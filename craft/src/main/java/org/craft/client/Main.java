package main.java.org.craft.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class of client application for Boldyreva experiment.
 *
 */
public final class Main {

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	/**
	 * Adress of target server.
	 */
	private static String url = "127.0.0.1";

	/**
	 * Starting time of algorithm.
	 */
	private static long start;

	/**
	 * Private default constructor.
	 */
	private Main() {

	}

	/**
	 * Main method.
	 * 
	 * @param args following arguments:
	 * 1 - url
	 * 2 - create/fill/select
	 * 3 - boldyreva_on or boldyreva_off
	 * 4 - encryption port
	 * 5 - decryption port
	 * 6 - nocryption port
	 */
	public static void main(final String[] args) {
		if (args.length > 1) {
			url = args[0];
			boolean ssl = false;
			if ("on".equals(args[3])) {
				ssl = true;
			}
			int ep = Integer.parseInt(args[4]);
			int dp = Integer.parseInt(args[5]);
			int np = Integer.parseInt(args[6]);
			start = System.currentTimeMillis();
			LOGGER.info("Started " + start);
			SQLScriptrunner runner = new SQLScriptrunner(url, "craft", ssl, ep, dp, np);
			switch (args[1]) {
			case "create":
				runner.runCreateDBScript();
				break;
			case "fill":
				runner.runFillDBScript(args[2]);
				break;
			case "select":
				runner.runSelectDBScript(args[2]);
				break;

			default:
				break;
			}
			runner.closeConnections();
		}
		long end = System.currentTimeMillis();
		LOGGER.info("Ended " + end + ". Duration: " + (end - start) + "ms.");
	}

	/**
	 * Provides the starting time of operation.
	 * 
	 * @return startting time of operation
	 */
	public static long getStarttime() {
		return start;
	}

}
