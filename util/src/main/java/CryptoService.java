package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * An object of this class can communicate with a cryto service on a given Port.
 */
public class CryptoService {

	/**
	 * Socket for communication.
	 */
	private Socket socket = null;
	/**
	 * Outgoing stream.
	 */
	private PrintWriter outStream;
	/**
	 * Ingoing Stream.
	 */
	private BufferedReader inStream;
	
	/**
	 * Counts the number of calls.
	 */
	private int calls = 0;
	
	/**
	 * Constructor, creates streams to the given Port.
	 * @param port . Port to communicate with
	 */
	public CryptoService(final int port) {		
		try {
			socket = new Socket("localhost", port);

			outStream = new PrintWriter(socket.getOutputStream(), true);
			inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		} catch (UnknownHostException e) {
			System.out.println("Unknown Host...");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO problems");
			e.printStackTrace();
		}
	}

	/**
	 * Sends the given value to the given Port.
	 * @param value - value to send
	 * @return encrypted or decrypted value, it depends on the service
	 */
	public long crypt(final long value) {
		try {
			calls++;
			outStream.println("" + value);
			outStream.flush();
			String answer = inStream.readLine();
			return Long.parseLong(answer);
		} catch (IOException e) {
			System.out.println("IO problems");
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * Closes the socket.
	 */
	public void closeSocket() {
		if (socket != null) {
			try {
				socket.close();
				System.out.println("Socket closed...");
			} catch (IOException e) {
				System.out.println("Unable to close socket");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Provides the number of calls.
	 * @return number of calls.
	 */
	public int getCalls() {
		return calls;
	}
}
