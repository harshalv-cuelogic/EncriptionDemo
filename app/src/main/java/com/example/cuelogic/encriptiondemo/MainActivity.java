package com.example.cuelogic.encriptiondemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        EncryptAndDecrypt obj = new EncryptAndDecrypt();
        JNCryptorEandD obj = new JNCryptorEandD();
//        AndroJNCryptorEandD obj = new AndroJNCryptorEandD();
//        obj.setKey();
//        String strToEncrypt = "{\"i\":\"10260177503\",\"e\":\"amitb@iprogrammer.com\"}";
//        String strToEncrypt = "{itinerary_No=1027506603&email_id=mayur.sojrani@cuelogic.com}";
//        Log.i("strToEncrypt: ", strToEncrypt);
        String strToDecrypt = "AwG5ey8jEyjrX+SLDVQrJp5ihP55+U4+00/MrNRZ088xpLdl0UtQGDzUFXquKwsvp1F8C/mdQbIN9XSVCRYTJkU7+s42xypjMgCBZCN1SC1w0au8quLOBNwqzkYz2Du6H2yQlRBd6Lx41rgCbXB3r1AmVWy0wIftn710GK23tv5e6g==";
        Log.i("strToDecrypt: ", strToDecrypt);
        String originalData = obj.decrypt(strToDecrypt);
        Log.i("originalData: ", originalData);
    }
}
