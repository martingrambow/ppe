package test.java.org.metadata;

import java.util.Scanner;

import junit.framework.TestCase;
import main.java.org.metadata.client.Oracle;
import main.java.org.metadata.server.POPEServer;

/**
 * Tests the communication between a Pope server and an oracle.
 */
public class TestOracleServerCommunication extends TestCase {

	/**
	 * Tests communication between a Pope server and an oracle.
	 */
	public void testCommunication() {
		String password = "Pudding";
		Oracle oracle = new Oracle("localhost", 1600, password, "test");
		oracle.startOracle();
		POPEServer server = new POPEServer("localhost", 1600, "localhost", 1700, "test");
		server.startPopeServer();
		
		//Do something...
		System.out.println("Press something to stop.");
		Scanner in = new Scanner(System.in);
		in.nextLine();
		in.close();
		
		server.stopPopeServer();
		oracle.stopOracle();
		
	}
}
