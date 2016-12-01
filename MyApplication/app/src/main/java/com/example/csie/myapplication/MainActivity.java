package com.example.csie.myapplication;

import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;


public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://192.168.1.33/conn.php";
    private   AsyncHttpClient Client = new AsyncHttpClient();





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




    RequestParams params = new RequestParams();
        params.setUseJsonStreamer(true);
        JSONObject jsonObject = new JSONObject();
        int a =123;
        try{
            jsonObject.put("x", a);
            jsonObject.put("y", 456);
            jsonObject.put("z",789);
        }catch (JSONException e){
            e.printStackTrace();
        }
        params.put("data",jsonObject);

while (true) {
    Client.post(getApplicationContext(), BASE_URL, null, params, RequestParams.APPLICATION_JSON, new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

        }
    });
}


}



   /* private void makePostRequest(){
        bt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               tv_display.setText(ed_string.getText());
            }
        });
        HttpClient httpclient = new DefaultHttpClient();
        String string1 = "789";
        while(true) {
            HttpPost httppost = new HttpPost("http://192.168.1.33:80/conn.php" + tv_display.getText().toString());
            try {
                httpclient.execute(httppost);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

    /*public class Rush extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {

            return null;
        }
    }*/


}



