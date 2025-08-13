package home.lib.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 
 * 
 * 
 * String key = "abcdefghijklmop";<br>
 * String clean = "Quisque eget odio ac lectus vestibulum faucibus eget."+h;<br>
 * <br>
 * byte[] encrypted = encrypt(clean.getBytes(), key);<br>
 * String decrypted = new String( decrypt(encrypted, key) );<br>
 * 
 * 
 * @author richardpark
 *
 */
public class CryptoUtil {

	public static void main(String[] args) {

		try {

			test(null);
			test2(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void test(String[] args) throws Exception {

		for (int h = 0; h < 10; h++) {
			long s = System.nanoTime();

			String key = "abcdefghijklmop";
			String clean = "화된 코드를 복호화 합니다. it decrypts the encrypted code." + h;

			byte[] encrypted = encrypt(clean.getBytes(), key);
			String decrypted = new String(decrypt(encrypted, key));

			long e = System.nanoTime();

			System.out.format("[%d] =>  %s : %s ms \r\n", encrypted.length, decrypted, ((double) (e - s) / 1000000));
		}
	}

	public static void test2(String[] args) throws Exception {

		String plainText = "Hello, World!";

		String key = "secret key";

		System.out.println("MD5 : " + plainText + " - " + CryptoUtil.md5(plainText));

		System.out.println("SHA-256 : " + plainText + " - " + CryptoUtil.sha256(plainText));

		for (int i = 0; i < 10; i++) {
			long s = System.nanoTime();

			CryptoUtil cu = new CryptoUtil();

			String encrypted = cu.encryptAES256("Hello, World!", key);

			String decrypted = cu.decryptAES256(encrypted, key);

			long e = System.nanoTime();

			System.out.println("AES-256 : enc - " + encrypted);

			System.out.println("AES-256 : dec - " + decrypted + "  " + ((double) (e - s) / 1000000) + " ms ");

		} // for(

	}

	/**
	 * 
	 * @param clean
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static byte[] encrypt(byte[] clean, String key) throws Exception {
		// byte[] clean = plainText.getBytes();

		// Generating IV.
		int ivSize = 16;
		byte[] iv = new byte[ivSize];
		SecureRandom random = new SecureRandom();
		random.nextBytes(iv);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

		// Hashing key.
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(key.getBytes("UTF-8"));
		byte[] keyBytes = new byte[16];
		System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
		SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

		// Encrypt.
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
		byte[] encrypted = cipher.doFinal(clean);

		// Combine IV and encrypted part.
		byte[] encryptedIVAndText = new byte[ivSize + encrypted.length];
		System.arraycopy(iv, 0, encryptedIVAndText, 0, ivSize);
		System.arraycopy(encrypted, 0, encryptedIVAndText, ivSize, encrypted.length);

		return encryptedIVAndText;
	}

	/**
	 * 
	 * 
	 * @param encryptedIvTextBytes
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static byte[] decrypt(byte[] encryptedIvTextBytes, String key) throws Exception {
		int ivSize = 16;
		int keySize = 16;

		// Extract IV.
		byte[] iv = new byte[ivSize];
		System.arraycopy(encryptedIvTextBytes, 0, iv, 0, iv.length);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

		// Extract encrypted part.
		int encryptedSize = encryptedIvTextBytes.length - ivSize;
		byte[] encryptedBytes = new byte[encryptedSize];
		System.arraycopy(encryptedIvTextBytes, ivSize, encryptedBytes, 0, encryptedSize);

		// Hash key.
		byte[] keyBytes = new byte[keySize];
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(key.getBytes());
		System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.length);
		SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

		// Decrypt.
		Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
		byte[] decrypted = cipherDecrypt.doFinal(encryptedBytes);

		// return new String(decrypted);
		return decrypted;
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * @param msg
	 * @return
	 * @throws NoSuchAlgorithmException
	 */

	int iterationCount = 3;

	/**
	 * 
	 * @param msg
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static String md5(String msg) throws NoSuchAlgorithmException {

		MessageDigest md = MessageDigest.getInstance("MD5");

		md.update(msg.getBytes());

		return CryptoUtil.byteToHexString(md.digest());

	}

	/**
	 * 
	 * @param msg
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static String sha256(String msg) throws NoSuchAlgorithmException {

		MessageDigest md = MessageDigest.getInstance("SHA-256");

		md.update(msg.getBytes());

		return CryptoUtil.byteToHexString(md.digest());

	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public static String byteToHexString(byte[] data) {

		StringBuilder sb = new StringBuilder();

		for (byte b : data) {

			sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));

		}

		return sb.toString();

	}

	/**
	 * 
	 * @param msg
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public String encryptAES256(String msg, String key) throws Exception {

		int iterationCount = 3;

		SecureRandom random = new SecureRandom();

		byte bytes[] = new byte[20];

		random.nextBytes(bytes);

		byte[] saltBytes = bytes;

		// Password-Based Key Derivation function 2

		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

		// 70000번 해시하여 256 bit 길이의 키를 만든다.

		PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), saltBytes, iterationCount, 256);

		SecretKey secretKey = factory.generateSecret(spec);

		SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

		// 알고리즘/모드/패딩

		// CBC : Cipher Block Chaining Mode

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

		cipher.init(Cipher.ENCRYPT_MODE, secret);

		AlgorithmParameters params = cipher.getParameters();

		// Initial Vector(1단계 암호화 블록용)

		byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();

		byte[] encryptedTextBytes = cipher.doFinal(msg.getBytes("UTF-8"));

		byte[] buffer = new byte[saltBytes.length + ivBytes.length + encryptedTextBytes.length];

		System.arraycopy(saltBytes, 0, buffer, 0, saltBytes.length);

		System.arraycopy(ivBytes, 0, buffer, saltBytes.length, ivBytes.length);

		System.arraycopy(encryptedTextBytes, 0, buffer, saltBytes.length + ivBytes.length, encryptedTextBytes.length);

		return Base64.getEncoder().encodeToString(buffer);

	}

	/**
	 * 
	 * @param msg
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public String decryptAES256(String msg, String key) throws Exception {

		int iterationCount = 3;

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

		ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(msg));

		byte[] saltBytes = new byte[20];

		buffer.get(saltBytes, 0, saltBytes.length);

		byte[] ivBytes = new byte[cipher.getBlockSize()];

		buffer.get(ivBytes, 0, ivBytes.length);

		byte[] encryoptedTextBytes = new byte[buffer.capacity() - saltBytes.length - ivBytes.length];

		buffer.get(encryoptedTextBytes);

		// System.out.format("msg.decode=%s chipper.blocksize(%s) buffer.len(%s) \r\n",
		// Base64.getDecoder().decode(msg).length , cipher.getBlockSize() , buffer.capacity() );

		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

		PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), saltBytes, iterationCount, 256);

		SecretKey secretKey = factory.generateSecret(spec);

		SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

		cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));

		byte[] decryptedTextBytes = cipher.doFinal(encryoptedTextBytes);

		return new String(decryptedTextBytes);

	}

}