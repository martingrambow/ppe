package main.java;

/**
 * Helperclass to measure durations.
 *
 */
public class TimeKeeper {
	
	/**
	 * Start time.
	 */
	private long start = -1;
	
	/**
	 * Current duration.
	 */
	private long duration = 0;
	
	/**
	 * Gets current measured duration.
	 * @return duration in ms
	 */
	public long getDuration() {
		if (start == -1) {
			return duration;
		}
		return duration + System.currentTimeMillis() - start;
	}
	
	/**
	 * Starts measurement.
	 */
	public void start() {
		start = System.currentTimeMillis();
	}

	/**
	 * Stops measurement.
	 */
	public void stop() {
		long round = System.currentTimeMillis() - start;
		duration = duration + round;
		start = -1;
	}
}
