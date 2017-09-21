package com.example.cuelogic.encriptiondemo;

/**
 * Created by cuelogic on 29/05/17.
 */

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptAndDecrypt {
    private static SecretKeySpec secretKey;
    private byte[] key;
    String myKey = "bcb04b7e103a0cd8b54763051cef0814";
    String decryptedString = "", encryptedString = "";
    String iv = "abcd123456789abc";
    public void setKey() {
        try {
            key = myKey.getBytes("UTF-8");
            secretKey = new SecretKeySpec(key, "AES");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            byte[] data = Base64.decode(strToDecrypt, Base64.DEFAULT);
            byte[] ivData = iv.getBytes();
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivData));
            decryptedString = new String(cipher.doFinal(data));
            decryptedString = decryptedString.substring(decryptedString.lastIndexOf("{"), (decryptedString.lastIndexOf("}") + 1));
        } catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return decryptedString;
    }

    public String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            byte[] ivData = iv.getBytes();
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(ivData));
            byte[] encryptedData = cipher.doFinal(strToEncrypt.getBytes());
            encryptedString = Base64.encodeToString(encryptedData, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedString;
    }
}