package test.java;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import main.java.BinDecConverter;

/**
 * Tests Bin <-> Dec conversion.
 */
public class TestConversion {

	/**
	 * Test conversion of positive numbers.
	 */
	@Test
	public void testConversionPositive() {
		int x = 5;
		int[] bin = BinDecConverter.decToBin(x, 8);
		int[] result = {1, 0, 1, 0, 0, 0, 0, 0};
		for (int i = 0; i < bin.length; i++) {
			assertTrue("error at position " + i, bin[i] == result[i]);
		}
		
		int convBack = BinDecConverter.binToDec(bin);
		assertTrue("dec -> bin -> dec not successful", convBack == x);
	}
	
	/**
	 * Tests conversion of negative numbers.
	 */
	@Test
	public void testConversionNegative() {
		int x = -5;
		int[] bin = BinDecConverter.decToBin(x, 8);
		int[] result = {1, 0, 1, 0, 0, 0, 0, 0};
		for (int i = 0; i < bin.length; i++) {
			System.out.println(i + ":" + bin[i]);
			assertTrue("error at position " + i, bin[i] == result[i]);
		}
		
		int convBack = BinDecConverter.binToDec(bin);
		assertTrue("dec -> bin -> dec not successful", convBack == x);
	}
}
