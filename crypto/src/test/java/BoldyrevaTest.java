package test.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import main.java.crypto.ope.BoldyrevaSystemCall;

/**
 * Tests the Boldyreva crytosystem implementation.
 */
public class BoldyrevaTest {

	/**
	 * Used Boldyreva object.
	 */
	private BoldyrevaSystemCall b;

	/**
	 * Random number generator for testnumbers.
	 */
	private static Random r = new Random();

	/**
	 * Create a Boldyreva object before each test.
	 * 
	 * @throws Exception
	 *             - in case of errors
	 */
	@Before
	public void setUp() throws Exception {
		b = new BoldyrevaSystemCall("Rosebud");
	}

	/**
	 * Tests simple encryption.
	 */
	@Test
	public void testIntegerEncryption() {
		int plain = 0;
		int count = 0;
		while (count < 20) {
			plain = r.nextInt(10000);
			long encrypted = b.encrypt(plain);
			assertTrue("encrypted value is negative", encrypted > 0);
			long decrypted = b.decrypt(encrypted);
			assertEquals("plain value (" + plain + ") is not decrypted value (" + decrypted + "), encrypted is " + encrypted, plain, decrypted);
			count++;
		}
	}
	
	/**
	 * Tests improved string encryption.
	 */
	@Test
	public void testStringEncryption() {
		String plain = "";
		int count = 0;
		String problem = "powder special 5819";
		String probemenc = b.encryptString(problem);
		String promblemdec = b.decryptString(probemenc);
		assertEquals("plain value is not same as decrypted value. encrypted is " + probemenc, problem, promblemdec);
		while (count < 40) {
			plain = getRandomString(35);
			String encrypted = b.encryptString(plain);
			assertTrue("encrypted value is null", encrypted != null);
			System.out.println("plain: " + plain + ", encrypted: " + encrypted);
			String decrypted = b.decryptString(encrypted);
			System.out.println("plain: " + plain + ", decrypted: " + decrypted);
			assertEquals("plain value is not same as decrypted value. encrypted is " + encrypted, plain, decrypted);
			count++;
		}
	}
	
	/**
	 * Tests improved string encryption with ordering.
	 */
	@Test
	public void testStringOrdering() {
		String plain1 = "";
		String plain2 = "";
		int count = 0;
		while (count < 20) {
			plain1 = getRandomString(15);
			plain2 = getRandomString(15);
			String encrypted1 = b.encryptString(plain1);
			String encrypted2 = b.encryptString(plain2);
			if (plain1.compareTo(plain2) < 0) {
				assertTrue("lexicographically order is not preserved", encrypted1.compareTo(encrypted2) < 0);
			}
			if (plain1.compareTo(plain2) > 0) {
				assertTrue("lexicographically order is not preserved", encrypted1.compareTo(encrypted2) > 0);
			}
			count++;
		}
	}
	
	
	
	/**
	 * Provides a random String.
	 * @param maxlength - maximum length of String (incl)
	 * @return random string
	 */
	private String getRandomString(final int maxlength) {
		String plain = "";
		int length = r.nextInt(maxlength) + 1;
		while (plain.length() < length) {
			boolean found = false;
			int c = 0;
			while (!found) {
				c = r.nextInt(123);
				if (c >= 32 && c <= 122) {
					found = true;
				}
			}
			char next = (char) c;
			plain = plain + next;
		}
		return plain;
	}

	/**
	 * Tests integer reference operation.
	 */
	@Test
	public void testNocrypt() {
		assertEquals(1337, b.nocrypt(1337));
	}

	/**
	 * Tests text en-/decryption reference operation.
	 */
	@Test
	public void testNocryptString() {
		assertEquals("chocolatecake", b.nocryptString("chocolatecake"));
	}

}
