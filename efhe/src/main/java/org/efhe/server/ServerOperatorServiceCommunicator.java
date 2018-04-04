package main.java.org.efhe.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object of this class can communicate with a EFHE operation server.
 */
public class ServerOperatorServiceCommunicator {

	/**
	 * Port for communication.
	 */
	private int port;

	/**
	 * Stats for operations, (AND, OR, NAND, NOR, XOR, NOT).
	 */
	private int[] statsOP = new int[6];
	/**
	 * Stats for parsing, (AND, OR, NAND, NOR, XOR, NOT).
	 */
	private int[] statsPARSE = new int[6];
	/**
	 * Stats for counts, (AND, OR, NAND, NOR, XOR, NOT).
	 */
	private int[] statsCOUNT = new int[6];

	/**
	 * Logger of this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerOperatorServiceCommunicator.class);

	/**
	 * Constructor, creates streams to the given Port.
	 * 
	 * @param port
	 *            . Port to communicate with
	 */
	public ServerOperatorServiceCommunicator(final int port) {
		this.port = port;
	}

	/**
	 * Sends the given text to the given Port.
	 * 
	 * @param value
	 *            - value to send
	 * @return Answer of service
	 */
	public String sendText(final String value) {
		String answer = "error";
		try {
			Socket socket = new Socket("localhost", port);
			PrintWriter outStream = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			try {
				outStream.println("" + value);
				outStream.flush();
				answer = inStream.readLine();

				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						LOGGER.error("Unable to close socket: ", e);
					}
				}

				if (answer == null) {
					LOGGER.error("answer was null, sent text: " + value);
					return "error";
				}
			} catch (IOException e) {
				LOGGER.error("IO Problems: ", e);
			}
		} catch (UnknownHostException e) {
			LOGGER.error("Unknown host:", e);
		} catch (IOException e) {
			if (e.getMessage().contains("Verbindungsaufbau abgelehnt")) {
				LOGGER.error("Unable to connect, adress is localhost:" + port);
			} else {
				LOGGER.error("IO problems: ", e);
			}
		}
		handleAnswer(answer);
		return answer;
	}

	/**
	 * Handles answer from Operator and updates statistics.
	 * 
	 * @param answer
	 *            - answer from server
	 */
	private void handleAnswer(final String answer) {
		String[] parts = answer.split(":");
		if (parts.length > 3) {
			int op = Integer.parseInt(parts[2]);
			int parse = Integer.parseInt(parts[3]) - op;
			// Update stats (AND, OR, NAND, NOR, XOR)
			switch (parts[1]) {
			case "AND":
				statsOP[0] = statsOP[0] + op;
				statsPARSE[0] = statsPARSE[0] + parse;
				statsCOUNT[0] = statsCOUNT[0] + 1;
				break;
			case "OR":
				statsOP[1] = statsOP[1] + op;
				statsPARSE[1] = statsPARSE[1] + parse;
				statsCOUNT[1] = statsCOUNT[1] + 1;
				break;
			case "NAND":
				statsOP[2] = statsOP[2] + op;
				statsPARSE[2] = statsPARSE[2] + parse;
				statsCOUNT[2] = statsCOUNT[2] + 1;
				break;
			case "NOR":
				statsOP[3] = statsOP[3] + op;
				statsPARSE[3] = statsPARSE[3] + parse;
				statsCOUNT[3] = statsCOUNT[3] + 1;
				break;
			case "XOR":
				statsOP[4] = statsOP[4] + op;
				statsPARSE[4] = statsPARSE[4] + parse;
				statsCOUNT[4] = statsCOUNT[4] + 1;
				break;
			case "NOT":
				statsOP[5] = statsOP[5] + op;
				statsPARSE[5] = statsPARSE[5] + parse;
				statsCOUNT[5] = statsCOUNT[5] + 1;
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Prints stats.
	 * @param out - channel to print the stats.
	 */
	public void printStats(final PrintWriter out) {
		int allOP = statsOP[0] + statsOP[1] + statsOP[2] + statsOP[3] + statsOP[4] + statsOP[5];
		int allParse = statsPARSE[0] + statsPARSE[1] + statsPARSE[2] + statsPARSE[3] + statsPARSE[4] + statsPARSE[5];
		out.println("AND: " + statsCOUNT[0] + ", " + statsOP[0] + " , " + statsPARSE[0]);
		out.println("OR: " + statsCOUNT[1] + ", " + statsOP[1] + " , " + statsPARSE[1]);
		out.println("NAND: " + statsCOUNT[2] + ", " + statsOP[2] + " , " + statsPARSE[2]);
		out.println("NOR: " + statsCOUNT[3] + ", " + statsOP[3] + " , " + statsPARSE[3]);
		out.println("XOR: " + statsCOUNT[4] + ", " + statsOP[4] + " , " + statsPARSE[4]);
		out.println("NOT: " + statsCOUNT[5] + ", " + statsOP[5] + " , " + statsPARSE[5]);
		out.println("ALL: " + allOP + ", " + allParse);
	}
	
	/**
	 * Resets stats.
	 */
	public void resetStats() {
		statsOP = new int[6];
		statsPARSE = new int[6];
		statsCOUNT = new int[6];
	}
}
