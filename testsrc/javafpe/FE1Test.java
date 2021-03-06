package javafpe;

import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

/**
 * Various tests of the algorithm entry point.
 */
public class FE1Test {

	private static final byte[] TEST_IV = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04 };

	private static final byte[] TEST_KEY = { 0x20, 0x01, 0x30, 0x50, 0x60, 0x70 };

	/**
	 * Tests creation of every element of a small range (1000) with the same key and IV to ensure no clashes.
	 * 
	 * @throws FPEException if any problems occur (fails test).
	 */
	@Test
	public void testMultipleRoundTrips() throws FPEException {
		int range = 10000;
		BigInteger modulus = BigInteger.valueOf(range);

		Set<Long> foundNumbers = new HashSet<Long>(range);
		for (int i = 0; i < range; i++) {
			long encryptedValue = TestUtility.roundTripTest(modulus, i, TEST_KEY, TEST_IV);
			if (foundNumbers.contains(encryptedValue)) {
				fail(String.format("Encrypted %d and got %d which already was generated (out of %d values)", i, encryptedValue, foundNumbers.size()));
			}
			foundNumbers.add(encryptedValue);
		}
	}

	/**
	 * Sample code to round trip a number.
	 * @throws FPEException If anyone goes wrong.
	 */
	@Test
	public void demoRoundTrip() throws FPEException {
		FE1 fe1 = new FE1();
		
		// The range of plaintext and ciphertext values 
		BigInteger modulus = new BigInteger("9999999999999999", 10);
		
		// A value to encrypt 
		BigInteger plaintextValue = new BigInteger("4444333322221111", 10);
		
		// A key, that will be used with the HMAC(SHA256) algorithm, note that this is not secure!
		byte[] hmacKey = new byte[] { 0x10, 0x20, 0x10, 0x20, 0x10, 0x20, 0x10, 0x20};
		
		// An initialisation vector, or tweak, used in the algorithm.
		byte[] iv = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
		
		BigInteger encryptedValue = fe1.encrypt(modulus, plaintextValue, hmacKey, iv);
		BigInteger decryptedValue = fe1.decrypt(modulus, encryptedValue, hmacKey, iv);
		
		System.out.println(String.format("Encrypted %s to %s and decrypted to %s", plaintextValue, encryptedValue, decryptedValue));
	}

	/**
	 * Test that passing an invalid key is rejected properly.
	 * 
	 * @throws FPEException expected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidKey() throws FPEException {
		new FE1().encrypt(BigInteger.valueOf(32), BigInteger.valueOf(0), new byte[0], new byte[] { 1, 2, 3, 4, 5 });
	}

	/**
	 * Try a modulus that's too big for the library to handle (2^128).
	 * 
	 * @throws FPEException Expected exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testTooLargeModulus() throws FPEException {
		new FE1().encrypt(new BigInteger("100000000000000000000000000000000", 16), BigInteger.valueOf(0), new byte[] { 1, 2, 3, 4, 5 },
						new byte[] { 1, 2, 3, 4, 5 });
	}

	/**
	 * FE1 requires the factors of the modulus to be found and used in the algorithm, therefore prime moduli are not permitted.
	 * 
	 * @throws FPEException should be thrown.
	 */
	@Test(expected = FPEException.class)
	public void testPrimeModulusFails() throws FPEException {
		new FE1().encrypt(BigInteger.valueOf(10007), BigInteger.valueOf(0), TEST_KEY, TEST_IV);

	}

	/**
	 * Null IVs are not permitted, must be >=1 byte long.
	 * 
	 * @throws FPEException unused.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNullIV() throws FPEException {
		new FE1().encrypt(BigInteger.valueOf(10007), BigInteger.valueOf(0), TEST_KEY, null);
	}

	/**
	 * Zero length IVs are not permitted.
	 * 
	 * @throws FPEException unused.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testZeroLengthIV() throws FPEException {
		new FE1().encrypt(BigInteger.valueOf(10007), BigInteger.valueOf(0), TEST_KEY, new byte[0]);
	}

	/**
	 * Test different length IVs, as long as it's greater than 1 byte long, it should be fine.
	 * 
	 * @throws FPEException If anything goes wrong.
	 */
	@Test
	public void testDifferentLengthIVs() throws FPEException {
		Random r = new Random(0);
		BigInteger modulus = BigInteger.valueOf(10000);
		for (int i = 1; i < 100; i++) {
			byte[] iv = new byte[i];
			r.nextBytes(iv);
			TestUtility.roundTripTest(modulus, 1, TEST_KEY, iv);
		}
	}

	/**
	 * Modulus must be less than 128 ^ 8, this is the limit of the algorithm.
	 * 
	 * @throws FPEException expected.
	 */
	@Test(expected = FPEException.class)
	public void testTooBigModulus() throws FPEException {
		new FE1().encrypt(BigInteger.valueOf((128 ^ 8) + 1), BigInteger.valueOf(0), TEST_KEY, TEST_IV);
	}

	/**
	 * Cannot encrypt data bigger than the modulus (one too big).
	 * 
	 * @throws FPEException expected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncryptTooBigInputBy1() throws FPEException {
		new FE1().encrypt(BigInteger.valueOf(10000), BigInteger.valueOf(10000), TEST_KEY, TEST_IV);
	}

	/**
	 * Cannot encrypt data bigger than the modulus (two too big).
	 * 
	 * @throws FPEException expected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncryptTooBigInputBy2() throws FPEException {
		new FE1().encrypt(BigInteger.valueOf(10000), BigInteger.valueOf(10001), TEST_KEY, TEST_IV);
	}

	/**
	 * Cannot decrypt data bigger than the modulus (one too big).
	 * 
	 * @throws FPEException expected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testDecryptTooBigInputBy1() throws FPEException {
		new FE1().decrypt(BigInteger.valueOf(10000), BigInteger.valueOf(10000), TEST_KEY, TEST_IV);
	}

	/**
	 * Cannot encrypt data bigger than the modulus (two too big).
	 * 
	 * @throws FPEException expected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testDecryptTooBigInputBy2() throws FPEException {
		new FE1().decrypt(BigInteger.valueOf(10000), BigInteger.valueOf(10001), TEST_KEY, TEST_IV);
	}

	/**
	 * Modulus must not be null.
	 * 
	 * @throws FPEException expected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncryptNullModulus() throws FPEException {
		new FE1().encrypt(null, BigInteger.valueOf(10001), TEST_KEY, TEST_IV);
	}

	/**
	 * plaintext must not be null.
	 * 
	 * @throws FPEException expected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testEncryptNullValue() throws FPEException {
		new FE1().encrypt(BigInteger.valueOf(10001), null, TEST_KEY, TEST_IV);
	}

	/**
	 * Modulus must not be null.
	 * 
	 * @throws FPEException expected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testDecryptNullModulus() throws FPEException {
		new FE1().decrypt(null, BigInteger.valueOf(10001), TEST_KEY, TEST_IV);
	}

	/**
	 * Ciphertext must not be null.
	 * 
	 * @throws FPEException expected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testDecryptNullValue() throws FPEException {
		new FE1().decrypt(BigInteger.valueOf(10001), null, TEST_KEY, TEST_IV);
	}

}
