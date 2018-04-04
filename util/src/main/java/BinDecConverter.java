package main.java;

/**
 * Helperclass to convert binary values.
 *
 */
public final class BinDecConverter {
	
	/**
	 * Default private constructor.
	 */
	private BinDecConverter() {
		
	}
	
	/**
	 * Converts decimal to binary.
	 * @param decimal - decimal value
	 * @param bits - number of bits
	 * @return binary representation
	 */
	public static int[] decToBin(final int decimal, final int bits) {
		int[] result = new int[bits];
		boolean negative = false;
		if (decimal < 0) {
			negative = true;
		}
		int curr = decimal;
		if (negative) {
			curr = curr * -1;
		}
		
		int pos = 0;
		while (curr > 0 && pos < bits) {
			int r = curr % 2;
			result[pos] = r;
			pos++;
			curr = curr / 2;
		}		
		if (negative) {
			//Build 2's complement
			for (int i = 0; i < bits; i++) {
				//Negate all bits
				if (result[i] == 0) {
					result[i] = 1;
				} else {
					result[i] = 0;
				}
			}
			//add +1
			boolean added = false;
			int i = 0;
			while (!added && (i < bits)) {
				if (result[i] == 0) {
					result[i] = 1;
					added = true;
				} else {
					result[i] = 0;
				}
				i++;
			}
		}
		return result;
	}
	
	/**
	 * Converts decimal to binary.
	 * @param binary - binary representation
	 * @return decimal value
	 */
	public static int binToDec(final int[] binary) {
		boolean negative = false;
		if (binary[binary.length - 1] == 1) {
			//negative number in 2's complement
			negative = true;
			//sub -1
			boolean subtracted = false;
			int i = 0;
			while (!subtracted && (i < binary.length)) {
				if (binary[i] == 1) {
					binary[i] = 0;
					subtracted = true;
				} else {
					binary[i] = 1;
				}
				i++;
			}
			//negate all bits
			for (int j = 0; j < binary.length; j++) {
				if (binary[j] == 0) {
					binary[j] = 1;
				} else {
					binary[j] = 0;
				}
			}
		}
		String bin = "";
		for (int i = 0; i < binary.length; i++) {
			bin = binary[i] + bin;
		}
		int result = Integer.parseInt(bin, 2);
		if (negative) {
			result = result * -1; 
		}
		return result;
	}

}
