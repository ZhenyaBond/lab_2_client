package by.bstu.fit.ben.lab_2_client;

import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import static by.bstu.fit.ben.lab_2_client.GPSTracker.latitude;
import static by.bstu.fit.ben.lab_2_client.GPSTracker.longitude;

public class MainActivity extends AppCompatActivity {

    public  static GPSTracker gps;
    TextView hashView;
    String time = "", str = "",encdata = "", ID = "";
    StringBuffer res = null;
    SecretKeySpec sks = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hashView = (TextView) findViewById(R.id.textView);
        try {
            sks = getKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

    public SecretKeySpec getKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKeySpec sks = new SecretKeySpec((keyGenerator.generateKey()).getEncoded(), "AES");
        return sks;
    }
    public void Location(View view) {
        gps = new GPSTracker(MainActivity.this);
        DateFormat df = new SimpleDateFormat("HH:mm");
        Date d = new Date();
        if (gps.canGetLocation()) {
            latitude =  gps.getLatitude();
            longitude =  gps.getLongitude();
            time = df.format(d);
            Toast.makeText(getApplicationContext(), "Location - \nLat: " + latitude + "\nLong: " + longitude+time, Toast.LENGTH_LONG).show();
        } else {
            gps.showSettingsAlert();
        }
    }

    public void ClickHash(View view) {
        getHash();
    }


    private void getHash(){
        try{
            str = "time:"+time+",lat:"+latitude+",lng:"+longitude;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(str.getBytes());
            byte[] bytes = md.digest();
            StringBuilder hashed = new StringBuilder();
            for (byte byt : bytes)
                hashed.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));

            Toast.makeText(this,  hashed.toString().toUpperCase(), Toast.LENGTH_LONG).show();

            res = new StringBuffer(str).append(",hash:"+hashed.toString().toUpperCase());

        }catch (Exception c){}
    }

    public void cipher(){
        byte[] encodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, sks);
            encodedBytes = c.doFinal(res.toString().getBytes());
        } catch (Exception e) {
            Toast.makeText(this, "AES encryption error", Toast.LENGTH_SHORT).show();
        }
        encdata = Base64.encodeToString(encodedBytes, Base64.NO_WRAP);
    }


    public void onClickSend(View view) throws InterruptedIOException {
        cipher();
        ID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        new ServerAsync().execute();
    }
    private class ServerAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                sendHTTP(ID,encdata);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static boolean sendHTTP(String ID, String encodedMessage) throws IOException {

        if(!encodedMessage.equals(""))
        {
            URL url=new URL("http://192.168.1.104:3000/api/stuff?id="+ID+"&encdata="+encodedMessage);
            HttpURLConnection con=(HttpURLConnection)url.openConnection();
            if(con.getResponseCode()==HttpURLConnection.HTTP_OK)
            {
                return true;
            }
            else{
                return false;
            }
        }
        else {
            return false;
        }

    }
}
