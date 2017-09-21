package com.example.cuelogic.encriptiondemo;

/**
 * Created by cuelogic on 29/05/17.
 */

import android.util.Base64;

import com.cuelogic.androjncryptor.CryptorException;
import com.cuelogic.androjncryptor.InvalidHMACException;
import com.cuelogic.androjncryptor.JNCryptor;
import com.cuelogic.androjncryptor.JNCryptorFactory;
import com.cuelogic.androjncryptor.JNCryptorSettings;

public class AndroJNCryptorEandD {
//    String myKey = "bcb04b7e103a0cd8b54763051cef0814";
    String sCipher = "", sCipher64 = "";
    String password = "abcd123456789abc";

    public String decrypt(String strToDecrypt) {
        JNCryptor crypt = JNCryptorFactory.getCryptor();
        JNCryptorSettings jnCryptSettings = new JNCryptorSettings(1000);
//        byte[] b = Base64.decode(strToDecrypt, Base64.NO_WRAP);
        byte[] b = Base64.decode(strToDecrypt, Base64.DEFAULT);
        byte[] cipher = null;
        try {
            cipher = crypt.decryptData(b, "f00b4r".toCharArray(), jnCryptSettings);
        } catch (InvalidHMACException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CryptorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (cipher != null){
            sCipher = new String(cipher);
        }
        return sCipher;
    }

    public String encrypt(String strToEncrypt) {
        JNCryptor crypt = JNCryptorFactory.getCryptor();
        JNCryptorSettings jnCryptSettings = new JNCryptorSettings(1000);
        byte[] cipher = null;
        try {
            cipher = crypt.encryptData(
                    strToEncrypt.getBytes(),
                    password.toCharArray(),
                    jnCryptSettings);
        } catch (CryptorException e) {
            e.printStackTrace();
        }
        if (cipher != null){
            // Using Base64 utility help to encode the cipher-text!
//            sCipher64 = Base64.encodeToString(cipher, Base64.NO_WRAP);
            sCipher64 = Base64.encodeToString(cipher, Base64.DEFAULT);
        }
        return sCipher64;
    }
}