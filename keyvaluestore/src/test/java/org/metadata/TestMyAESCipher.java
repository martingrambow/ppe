package test.java.org.metadata;

import java.util.Random;

import junit.framework.TestCase;
import main.java.org.metadata.client.MyAESCipher;

/**
 * Tests AES cipher.
 *
 */
public class TestMyAESCipher extends TestCase {
	
	/**
	 * used cipher object.
	 */
	private MyAESCipher cipher = new MyAESCipher("Pudding");
	
	/**
	 * Used to generate random values.
	 */
	private Random r = new Random();
	
	/**
	 * tests integer encryption/decryption.
	 */
	public void testIntCipher() {
		
		for (int i = 0; i < 1000; i++) {
			int plain = r.nextInt(10000000);
			byte[] encrypted = cipher.encrypt("" + plain);
			String decrypted = cipher.decrypt(encrypted);
			int tmp = Integer.parseInt(decrypted);
			assertTrue("plain and decrypted not equal", tmp == plain);
		}		
	}
	
	/**
	 * Tests String encryption/decryption.
	 */
	public void testStringCipher() {
		for (int i = 0; i < 1000; i++) {
			int len = r.nextInt(100);
			String plain = "";
			while (len > 0) {
				char c = (char) (r.nextInt(127 - 48) + 48);
				plain = plain + c;
				len--;
			}
			byte[] encrypted = cipher.encrypt(plain);
			String decrypted = cipher.decrypt(encrypted);
			assertTrue("plain and decrypted not equal", plain.equals(decrypted));
		}
	}
	

}
