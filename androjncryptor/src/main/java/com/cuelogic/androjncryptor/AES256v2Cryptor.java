/*    Copyright 2013 Duncan Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cuelogic.androjncryptor;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

import android.os.Build;

/**
 * This {@link JNCryptor} instance produces data in version 2 format.
 * <p>
 * 
 * <pre>
 * | version | options | encryption salt | HMAC salt |   IV   | ... ciphertext ... |     HMAC    |
 * |    0    |    1    |       2->9      |   10->17  | 18->33 | <-      ...     -> | (n-32) -> n |
 * </pre>
 * 
 * <ul>
 * <li><b>version</b> (1 byte): Data format version. Always {@code 0x02}.</li>
 * <li><b>options</b> (1 byte): {@code 0x00} if keys are used, {@code 0x01} if a
 * password is used.</li>
 * <li><b>encryption salt</b> (8 bytes)</li>
 * <li><b>HMAC salt</b> (8 bytes)</li>
 * <li><b>IV</b> (16 bytes)</li>
 * <li><b>ciphertext</b> (variable): 256-bit AES encrypted, CBC-mode with
 * PKCS&nbsp;#5 padding.</li>
 * <li><b>HMAC</b> (32 bytes)</li>
 * </ul>
 * 
 * <p>
 * The encryption key is derived using the PKBDF2 function, using a random
 * eight-byte encryption salt, the supplied password and 10,000 iterations. The
 * HMAC key is derived in a similar fashion, using it's own random eight-byte
 * HMAC salt. Both salt values are stored in the ciphertext output (as shown
 * above).
 * 
 * <p>
 * The ciphertext is AES-256-CBC encrypted, using a randomly generated IV and
 * the encryption key (described above), with PKCS&nbsp;#5 padding.
 * <p>
 * The HMAC is calculated across all the data (except the HMAC itself, of
 * course), generated using the HMAC key described above and the SHA-256 PRF.
 * <p> 
 * See <a href="https://github.com/rnapier/RNCryptor/wiki/Data-Format">https://github.com/rnapier/RNCryptor/wiki/Data-Format</a>, 
 * from which most of the information above was shamelessly copied.
 */
public class AES256v2Cryptor implements JNCryptor {

	
	
  private static final String AES_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String AES_NAME = "AES";
  private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1";
  private static final int PBKDF_ITERATIONS = 10000; 
  //
  private static final int VERSION = 2;
  private static final int AES_256_KEY_SIZE = 256 / 8;
  private static final int AES_BLOCK_SIZE = 16;
  //
  // Default settings
  private JNCryptorSettings mCryptorSettings = new JNCryptorSettings(PBKDF_ITERATIONS);

  // Salt length exposed as package private to aid unit testing
  static final int SALT_LENGTH = 8;

  // SecureRandom is threadsafe
  private static SecureRandom SECURE_RANDOM;

  static {
    // Register this class with the factory
    JNCryptorFactory.registerCryptor(VERSION, new AES256v2Cryptor());
    // Bug fix for those on JB with change to Secure Random 
    // (http://stackoverflow.com/questions/13383006/encryption-error-on-android-4-2)
    if (Build.VERSION.SDK_INT >= 17)
	    try {
	      SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG", "Crypto");
      } catch (NoSuchAlgorithmException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
      } catch (NoSuchProviderException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
      }
    else
	    try {
	      SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");
      } catch (NoSuchAlgorithmException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
      }
  }

  /**
   * This class should be accessed only via
   * {@link JNCryptorFactory#getCryptor()}, except for unit testing.
   */
  AES256v2Cryptor() {
  }

  @Override
  public SecretKey keyForPassword(char[] password, byte[] salt)
      throws CryptorException {

  	if (salt == null) throw new IllegalArgumentException("Salt value cannot be null.");
  	if (salt.length != SALT_LENGTH) 
  		throw new IllegalArgumentException(String.format("Salt value must be %d bytes.", SALT_LENGTH));
    try {
    	if (Build.VERSION.SDK_INT < 8){
    		PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA1Digest());
    		generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password), salt, this.mCryptorSettings.getRounds());
    		KeyParameter key = (KeyParameter)generator.generateDerivedMacParameters(AES_256_KEY_SIZE * 8);
    		byte[] bKey = key.getKey();
    		return new SecretKeySpec(bKey, 0, bKey.length, AES_NAME);
    	}else{
	      SecretKeyFactory factory = SecretKeyFactory
	          .getInstance(KEY_DERIVATION_ALGORITHM);
	      SecretKey tmp = factory.generateSecret(new PBEKeySpec(password, salt,
	      		this.mCryptorSettings.getRounds(), AES_256_KEY_SIZE * 8));
	      return new SecretKeySpec(tmp.getEncoded(), AES_NAME);
    	}
    } catch (GeneralSecurityException e) {
      throw new CryptorException(String.format(
          "Failed to generate key from password using %s.",
          KEY_DERIVATION_ALGORITHM), e);
    }
    
  }

  /**
   * Decrypts data.
   * 
   * @param aesCiphertext
   *          the ciphertext from the message
   * @param decryptionKey
   *          the key to decrypt
   * @param hmacKey
   *          the key to recalculate the HMAC
   * @return the decrypted data
   * @throws CryptorException
   *           if a JCE error occurs
   */
  private byte[] decryptData(AES256v2Ciphertext aesCiphertext,
      SecretKey decryptionKey, SecretKey hmacKey) throws CryptorException {

    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(hmacKey);
      byte[] hmacValue = mac.doFinal(aesCiphertext.getDataToHMAC());

      if (!Arrays.equals(hmacValue, aesCiphertext.getHmac())) {
        throw new InvalidHMACException("Incorrect HMAC value.");
      }

      Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, decryptionKey, new IvParameterSpec(
          aesCiphertext.getIv()));

      return cipher.doFinal(aesCiphertext.getCiphertext());
    } catch (GeneralSecurityException e) {
      throw new CryptorException("Failed to decrypt message.", e);
    }
  }

  @Override
  public byte[] decryptData(byte[] ciphertext, char[] password)
      throws CryptorException {
  	if (ciphertext == null) throw new IllegalArgumentException("Ciphertext cannot be null.");
 
    try {
      AES256v2Ciphertext aesCiphertext = new AES256v2Ciphertext(ciphertext);

      if (!aesCiphertext.isPasswordBased()) {
        throw new IllegalArgumentException(
            "Ciphertext was not encrypted with a password.");
      }

      SecretKey decryptionKey = keyForPassword(password,
          aesCiphertext.getEncryptionSalt());
      SecretKey hmacKey = keyForPassword(password, aesCiphertext.getHmacSalt());

      return decryptData(aesCiphertext, decryptionKey, hmacKey);
    } catch (InvalidDataException e) {
      throw new CryptorException("Unable to parse ciphertext.", e);
    }
  }
  
  @Override
  public byte[] decryptData(byte[] ciphertext, char[] password, JNCryptorSettings settings)
      throws CryptorException {
  	if (ciphertext == null) throw new IllegalArgumentException("Ciphertext cannot be null.");

    try {
    	//JNCryptorUtils.LOGGER("decryptData: Settings: " + this.mCryptorSettings.toString());
    	this.mCryptorSettings = settings;
    	//JNCryptorUtils.LOGGER("decryptData: Settings: " + this.mCryptorSettings.toString());
      AES256v2Ciphertext aesCiphertext = new AES256v2Ciphertext(ciphertext);

      if (!aesCiphertext.isPasswordBased()) {
        throw new IllegalArgumentException(
            "Ciphertext was not encrypted with a password.");
      }

      SecretKey decryptionKey = keyForPassword(password,
          aesCiphertext.getEncryptionSalt());
      SecretKey hmacKey = keyForPassword(password, aesCiphertext.getHmacSalt());

      return decryptData(aesCiphertext, decryptionKey, hmacKey);
    } catch (InvalidDataException e) {
      throw new CryptorException("Unable to parse ciphertext.", e);
    }
  }

  /**
   * Encrypts plaintext data, 256-bit AES CBC-mode with PKCS#5 padding.
   * 
   * @param plaintext
   *          the plaintext
   * @param password
   *          the password (can be <code>null</code> or empty)
   * @param encryptionSalt
   *          eight bytes of random salt value
   * @param hmacSalt
   *          eight bytes of random salt value
   * @param iv
   *          sixteen bytes of AES IV
   * @return a formatted ciphertext
   * @throws CryptorException
   *           if an error occurred
   */
  byte[] encryptData(byte[] plaintext, char[] password, byte[] encryptionSalt,
      byte[] hmacSalt, byte[] iv) throws CryptorException {

    SecretKey encryptionKey = keyForPassword(password, encryptionSalt);
    SecretKey hmacKey = keyForPassword(password, hmacSalt);

    try {
      Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new IvParameterSpec(iv));
      byte[] ciphertext = cipher.doFinal(plaintext);

      AES256v2Ciphertext output = new AES256v2Ciphertext(encryptionSalt, hmacSalt,
          iv, ciphertext);

      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(hmacKey);
      byte[] hmac = mac.doFinal(output.getDataToHMAC());
      output.setHmac(hmac);
      return output.getRawData();

    } catch (GeneralSecurityException e) {
      throw new CryptorException("Failed to generate ciphertext.", e);
    }
  }

  @Override
  public byte[] encryptData(byte[] plaintext, char[] password)
      throws CryptorException {
  	if (plaintext == null) throw new IllegalArgumentException("Plaintext cannot be null.");

    byte[] encryptionSalt = getSecureRandomData(SALT_LENGTH);
    byte[] hmacSalt = getSecureRandomData(SALT_LENGTH);
    byte[] iv = getSecureRandomData(AES_BLOCK_SIZE);

    return encryptData(plaintext, password, encryptionSalt, hmacSalt, iv);
  }
  
  @Override
  public byte[] encryptData(byte[] plaintext, char[] password, JNCryptorSettings settings)
      throws CryptorException {
    if (plaintext == null) throw new IllegalArgumentException("Plaintext cannot be null.");
    
    this.mCryptorSettings = settings;
    byte[] encryptionSalt = getSecureRandomData(SALT_LENGTH);
    byte[] hmacSalt = getSecureRandomData(SALT_LENGTH);
    byte[] iv = getSecureRandomData(AES_BLOCK_SIZE);

    return encryptData(plaintext, password, encryptionSalt, hmacSalt, iv);
  }

  /**
   * Returns random data supplied by this class' {@link SecureRandom} instance.
   * 
   * @param length
   *          the number of bytes to return
   * @return random bytes
   */
  private static byte[] getSecureRandomData(int length) {
    byte[] result = new byte[length];
    SECURE_RANDOM.nextBytes(result);
    return result;
  }

  @Override
  public int getVersionNumber() {
    return VERSION;
  }

  @Override
  public byte[] decryptData(byte[] ciphertext, SecretKey decryptionKey,
      SecretKey hmacKey) throws CryptorException, InvalidHMACException {
  	if (ciphertext == null) throw new IllegalArgumentException("Ciphertext cannot be null.");
  	if (decryptionKey == null) throw new IllegalArgumentException("Decryption key cannot be null.");
  	if (hmacKey == null) throw new IllegalArgumentException("HMAC key cannot be null.");
    
    AES256v2Ciphertext aesCiphertext;
    try {
      aesCiphertext = new AES256v2Ciphertext(ciphertext);

      return decryptData(aesCiphertext, decryptionKey, hmacKey);

    } catch (InvalidDataException e) {
      throw new CryptorException("Unable to parse ciphertext.", e);
    }
  }

  @Override
  public byte[] encryptData(byte[] plaintext, SecretKey encryptionKey,
      SecretKey hmacKey) throws CryptorException {

  	if (plaintext == null) throw new IllegalArgumentException("Plaintext cannot be null.");
  	if (encryptionKey == null) throw new IllegalArgumentException("Encryption key cannot be null.");
  	if (hmacKey == null) throw new IllegalArgumentException("HMAC key cannot be null.");
    
    byte[] iv = getSecureRandomData(AES_BLOCK_SIZE);

    try {
      Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new IvParameterSpec(iv));
      byte[] ciphertext = cipher.doFinal(plaintext);

      AES256v2Ciphertext output = new AES256v2Ciphertext(iv, ciphertext);

      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(hmacKey);
      byte[] hmac = mac.doFinal(output.getDataToHMAC());
      output.setHmac(hmac);
      return output.getRawData();

    } catch (GeneralSecurityException e) {
      throw new CryptorException("Failed to generate ciphertext.", e);
    }
  }

}
