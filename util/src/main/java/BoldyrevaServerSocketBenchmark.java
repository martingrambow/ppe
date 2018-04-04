package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Helperclass for Boldyreva Server Socket Benachmarking experiment.
 *
 */
public final class BoldyrevaServerSocketBenchmark {
	
	/**
	 * Private default constructor.
	 */
	private BoldyrevaServerSocketBenchmark() {
		
	}

	/**
	 * Starts experiment.
	 * @param args [0] - port, [1] - Start value, [2] - End value
	 */
	public static void main(final String[] args) {

		// start server manually!
		int port = Integer.parseInt(args[0]);
		int startvalue = Integer.parseInt(args[1]);
		int endvalue = Integer.parseInt(args[2]);

		Socket socket = null;
		try {
			socket = new Socket("localhost", port);

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			long start = System.currentTimeMillis();

			for (int i = startvalue; i < endvalue; i++) {
				out.println("" + i);
				out.flush();
				String answer = in.readLine();
				System.out.println("i = " + i + ": " + answer);
			}

			long end = System.currentTimeMillis();
			
			socket.close();			

			System.out.println("Duration = " + (end - start) + "ms");

		} catch (UnknownHostException e) {
			System.out.println("Unknown Host...");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO problems");
			e.printStackTrace();
		} finally {
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
	}
}
