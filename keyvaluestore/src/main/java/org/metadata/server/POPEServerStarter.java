package main.java.org.metadata.server;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts POPE servers.
 */
public final class POPEServerStarter {
	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(POPEServerStarter.class);
	
	/**
	 * Private default constructor.
	 */
	private POPEServerStarter() {
		
	}

	/**
	 * Starts 5 POPE servers.
	 * 
	 * @param args oracleHost oracleStartPort serverHost serverStartPort
	 */
	public static void main(final String[] args) {
		
		String oracleHost = args[0];
		int oraclePort = Integer.parseInt(args[1]);
		String host = args[2];
		int serverStartPort = Integer.parseInt(args[3]);
		
		POPEServer year = new POPEServer(oracleHost, oraclePort, host, serverStartPort, "year");
		POPEServer month = new POPEServer(oracleHost, oraclePort + 1, host, serverStartPort + 1, "month");
		POPEServer hour = new POPEServer(oracleHost, oraclePort + 2, host, serverStartPort + 2, "hour");
		POPEServer location = new POPEServer(oracleHost, oraclePort + 3, host, serverStartPort + 3, "location");
		POPEServer tag = new POPEServer(oracleHost, oraclePort + 4, host, serverStartPort + 4, "tag");
		
		year.startPopeServer();
		month.startPopeServer();
		hour.startPopeServer();
		location.startPopeServer();
		tag.startPopeServer();
		
		LOGGER.info("Press something to stop all POPE servers.");
		Scanner in = new Scanner(System.in);
		in.nextLine();
		in.close();

		year.stopPopeServer();
		month.stopPopeServer();
		hour.stopPopeServer();
		location.stopPopeServer();
		tag.stopPopeServer();

	}

}
