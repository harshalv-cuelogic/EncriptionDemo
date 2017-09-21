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

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Factory used to obtain {@link JNCryptor} instances. A different instance is
 * available for each version of the original RNCryptor library. The most modern
 * implementation is always available by calling:
 * <p>
 * 
 * <pre>
 * RNCryptor cryptor = RNCryptorFactory.getCryptor();
 * </pre>
 * <p>
 * If a particular version is required, it can be obtained with the
 * {@link #getCryptor(int) getCryptor(int version)} method. A full list of the
 * available instances is obtained through the {@link #getCryptors()} method.
 * <p>
 * If the required version is not known, the ciphertext can be inspected to find
 * the correct version using the {@link #getCryptorForCiphertext(byte[])}
 * method. This method searches for the version byte stored in the ciphertext.
 */
@SuppressWarnings("deprecation")
public class JNCryptorFactory {

  private static final SortedMap<Integer, JNCryptor> supportedVersions = new TreeMap<Integer, JNCryptor>();

  static {
	  Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
	  
    // Load classes defined in properties file
    try {
    	String str = "t0mm13b.AndroJNCryptor.AES256v2Cryptor\n";
      try {
        List<String> listOfClasses = JNCryptorUtils.readLines(str);
        for (String className : listOfClasses) {
          Class.forName(className);
          JNCryptorUtils.LOGGER("Loaded class {}." + className);
        }
      } finally {
        
      }

    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This is a class of static methods only.
   */
  private JNCryptorFactory() {
  }

  /**
   * Examines the first byte of the ciphertext, which contains a version byte.
   * Returns an {@link JNCryptor} that implements the settings for that version.
   * 
   * @param ciphertext
   *          the ciphertext to examine
   * @return an {@link JNCryptor} that supports that version
   * @throws ClassNotFoundException
   *           if the version does not correspond to a supported type
   */
  public static JNCryptor getCryptorForCiphertext(byte[] ciphertext)
      throws ClassNotFoundException {
  	if (ciphertext == null) throw new IllegalArgumentException("Ciphertext cannot be null.");
  	if (ciphertext.length < 1) throw new IllegalArgumentException("Ciphertext cannot be zero length.");

    int version = ciphertext[0] & 0xFF; // Convert to unsigned int
    return getCryptor(version);
  }

  /**
   * Registers a mapping between a version number and an {@link JNCryptor}.
   * 
   * @param version
   *          the version number
   * @param cryptor
   *          the {@link JNCryptor}
   */
  static void registerCryptor(int version, JNCryptor cryptor) {
    if (supportedVersions.containsKey(version)) {
      throw new IllegalStateException(String.format(
          "Support for version %#04x already exists.", version));
    }

    supportedVersions.put(version, cryptor);
    JNCryptorUtils.LOGGER("Cryptor registered with support for version {}." + version);
  }

  /**
   * Retrieves an {@link JNCryptor} implementing the current data format
   * (determined by seeking the implementation with the largest version number).
   * 
   * @return an {@link JNCryptor}
   */
  public static JNCryptor getCryptor() {
    if (supportedVersions.isEmpty()) {
      throw new IllegalStateException("No implementations registered.");
    }

    return supportedVersions.get(supportedVersions.lastKey());
  }

  /**
   * Retrieves an {@link JNCryptor} implementing the specified version number.
   * 
   * @param version
   *          the version number. A positive number smaller than 256 (must be
   *          expressible in eight bits).
   * @return the {@link JNCryptor}
   * @throws ClassNotFoundException
   *           if no implementation exists for that version
   */
  public static JNCryptor getCryptor(int version) throws ClassNotFoundException {
    JNCryptor cryptor = supportedVersions.get(version);

    if (cryptor == null) {
      throw new ClassNotFoundException(String.format(
          "No implementation found for version %d.", version));
    }

    return cryptor;
  }

  /**
   * Returns a list of the available {@link JNCryptor}s, arranged in ascending
   * order of version number.
   * 
   * @return an ordered list of {@code RNCryptor}s.
   */
  public static List<JNCryptor> getCryptors() {
    List<JNCryptor> result = new ArrayList<JNCryptor>(supportedVersions.size());

    for (Integer version : supportedVersions.keySet()) {
      result.add(supportedVersions.get(version));
    }

    return result;
  }

}
