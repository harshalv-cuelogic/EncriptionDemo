package com.example.cuelogic.encriptiondemo;

/**
 * Created by cuelogic on 29/05/17.
 */

import android.util.Base64;

import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.CryptorException;
import org.cryptonode.jncryptor.JNCryptor;

import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class JNCryptorEandD {
    private final String myKey = "bcb04b7e103a0cd8b54763051cef0814";
    private final String password = myKey;
    String decryptedString = "", encryptedString = "";

    public String decrypt(String strToDecrypt) {
        JNCryptor cryptor = new AES256JNCryptor();
        try {
            byte[] encryptedText = Base64.decode(strToDecrypt, Base64.DEFAULT);
            byte[] ciphertext = cryptor.decryptData(encryptedText, password.toCharArray());
            decryptedString = new String(ciphertext);
        } catch (CryptorException e) {
            // Something went wrong
            e.printStackTrace();
        }
        return decryptedString;
    }

    public String encrypt(String strToEncrypt) {
        JNCryptor cryptor = new AES256JNCryptor();
        byte[] plaintext = strToEncrypt.getBytes();
        try {
            byte[] ciphertext = cryptor.encryptData(plaintext, password.toCharArray());
            encryptedString = Base64.encodeToString(ciphertext, Base64.DEFAULT);
        } catch (CryptorException e) {
            // Something went wrong
            e.printStackTrace();
        }
        return encryptedString;
    }
}