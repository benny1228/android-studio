package com.cjcu06.gsensor;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager = null;
    private Sensor gyroSensor = null;
    private TextView vX;
    private TextView vY;
    private TextView vZ;
    private TextView v;
    private Button button;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private float[] angle = new float[3];
    private byte[] servo=new byte[10];
    private String push="0";
    private String push2="0";
    private TextView tvPush;

    private static final String BASE_URL = "http://192.168.51.2/conn.php";
    private   AsyncHttpClient Client = new AsyncHttpClient();


    private byte[] x, y, z;
    private byte[] Cmd;
    private int lstValue = 0;  //上次的值
    private int  curValue = 0;  //現在值


    @SuppressWarnings("deprecation")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvPush = (TextView)findViewById(R.id.push);

        //biuetooth到此

        vX = (TextView) findViewById(R.id.vx);
        vY = (TextView) findViewById(R.id.vy);
        vZ = (TextView) findViewById(R.id.vz);
        v = (TextView) findViewById(R.id.v);

        button = (Button) findViewById(R.id.button);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ORIENTATION);
        vX.setText("!!!!!!");
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                //聲明可變字符串
                StringBuffer sb = new StringBuffer();
                //獲取手機全部的傳感器
                List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
                //迭代輸出獲得上的傳感器
                for (Sensor sensor : sensors) {
                    //System.out.println(sensor.getName().toString());
                    sb.append(sensor.getName().toString());
                    sb.append("\n");
                    Log.i("Sensor", sensor.getName().toString());
                }
                //给文本控件賦值
                v.setText(sb.toString());
            }
        });

    }




        public MainActivity() {
        // TODO Auto-generated constructor stub
        angle[0] = 0;
        angle[1] = 0;
        angle[2] = 0;
        timestamp = 0;
    }


   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_sensor_test, menu);
        return true;
    }*/

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        sensorManager.unregisterListener(this); // 解除監聽器注冊
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);  //为傳感器注冊監聽器
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
//		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
//        {
//    	 return;
//        }

//    	if (timestamp != 0) {
//        	final float dT = (event.timestamp - timestamp) * NS2S;
//        	angle[0] += event.values[0] * dT * 100;
//        	angle[1] += event.values[1] * dT * 100;
//        	angle[2] += event.values[2] * dT * 100;
//         }
//         timestamp = event.timestamp;
//
//
//         vX.setText("X: " + Float.toString(angle[0]));
//         vY.setText("Y: " + Float.toString(angle[1]));
//         vZ.setText("Z: " + Float.toString(angle[2]));

//		方向傳感器提供三個數據，分別为azimuth、pitch和roll。
//
//		azimuth：方位，返回水平時磁北極和Y軸的夾角，範圍为0°至360°。
//		0°=北，90°=東，180°=南，270°=西。
//
//		pitch：x軸和水平面的夾角，範圍为-180°至180°。
//		當z軸向y軸轉動時，角度为正值。
//
//		roll：y軸和水平面的夾角，由於曆史原因，範圍为-90°至90°。
//		當x軸向z軸移動時，角度为正值。

        vX.setText(String.format("%.2f", event.values[0]));
        vY.setText(String.format("%.2f", event.values[1]));
        vZ.setText(String.format("%.2f", event.values[2]));

        byte[] a = String.format("%.2f", event.values[0]).getBytes(StandardCharsets.US_ASCII);
        byte[] b = String.format("%.2f", event.values[1]).getBytes(StandardCharsets.US_ASCII);
        byte[] c = String.format("%.2f", event.values[2]).getBytes(StandardCharsets.US_ASCII);

        Log.e("view x", "x:" + bytesToHex(a));
        Log.e("view a", "a:" + a);
        Log.e("View a", "a[0]" + a[0]);
        Log.e("View a", "a[1]" + a[1]);
        Log.e("View a", "a[2]" + a[2]);


        if (a.length < 6) {
            x = bluetoothpacket(a);
        } else if (a.length == 6) {
            x = a;
        }

        if (b.length < 6) {
            y = bluetoothpacket(b);
        } else if (b.length == 6) {
            y = b;
        }

        if (c.length < 6) {
            z = bluetoothpacket(c);
        } else if (c.length == 6) {
            z = c;
        }

        Cmd = new byte[23];

        Cmd[0] = (byte) 0xA0;
        Cmd[1] = (byte) 0x58;

        Cmd[2] = x[0];
        Cmd[3] = x[1];
        Cmd[4] = x[2];
        Cmd[5] = x[3];
        Cmd[6] = x[4];
        Cmd[7] = x[5];


        Cmd[8] = (byte) 0x59;

        Cmd[9] = y[0];
        Cmd[10] = y[1];
        Cmd[11] = y[2];
        Cmd[12] = y[3];
        Cmd[13] = y[4];
        Cmd[14] = y[5];

        Cmd[15] = (byte) 0x5A;

        Cmd[16] = z[0];
        Cmd[17] = z[1];
        Cmd[18] = z[2];
        Cmd[19] = z[3];
        Cmd[20] = z[4];
        Cmd[21] = z[5];

        Cmd[22] = (byte) 0xFB;
        Log.e("csie cmmd", "cmd:" + bytesToHex(Cmd));
   //z
        if (Cmd[16]==48 && Cmd[17]<=57 && Cmd[17]>=51 && Cmd[18]>=48 && Cmd[18]<=57 && Cmd[20]>=48 && Cmd[20]<=57){
            push2="000";
        }else if (Cmd[16]==48 && Cmd[17]<=51 && Cmd[17]>=48 && Cmd[18]>=48 && Cmd[18]<=57 && Cmd[20]>=48 && Cmd[20]<=57){
            push2="090";
        }else if (Cmd[16]==45 && Cmd[17]<=51 && Cmd[17]>=48 && Cmd[18]>=48 && Cmd[18]<=57 && Cmd[20]>=48 && Cmd[20]<=57){
            push2="090";
        }else if (Cmd[16]==45 && Cmd[17]<=57 && Cmd[17]>=51 && Cmd[18]>=48 && Cmd[18]<=57 && Cmd[20]>=48 && Cmd[20]<=57){
            push2="180";
        }

//y
    if (Cmd[9]==48 && Cmd[10]<=57 && Cmd[10]>=48 && Cmd[11]>=48 && Cmd[11]<=57 && Cmd[13]>=48 && Cmd[13]<=57){
        push="000";
    }else if (Cmd[9]==45 && Cmd[10]<=51 && Cmd[10]>=48 && Cmd[11]>=48 && Cmd[11]<=57 && Cmd[13]>=48 && Cmd[13]<=57){
        push="000";
    }else if (Cmd[9]==45 && Cmd[10]<=54 && Cmd[10]>=51 && Cmd[11]>=48 && Cmd[11]<=57 && Cmd[13]>=48 && Cmd[13]<=57){
        push="090";
    }else if (Cmd[9]==45 && Cmd[10]<=57 && Cmd[10]>=54 && Cmd[11]>=48 && Cmd[11]<=57 && Cmd[13]>=48 && Cmd[13]<=57){
        push="180";
    }

        Log.e("view push", "push:" + push);
        tvPush.setText(push+" "+push2);
        int newValue = Integer.parseInt(push);
        curValue = newValue;
        Log.e("post","post true:"+curValue);
        int newValueZ =Integer.parseInt(push2);
        lstValue =newValueZ;


            //post
            RequestParams params = new RequestParams();
            params.setUseJsonStreamer(true);
            JSONObject jsonObject = new JSONObject();
            try{
                jsonObject.put("name",123);
                jsonObject.put("x",lstValue);
                jsonObject.put("y", curValue);
                jsonObject.put("z",789);
            }catch (JSONException e){
                e.printStackTrace();
            }
            params.put("data",jsonObject);


            Client.post(getApplicationContext(), BASE_URL, null, params, RequestParams.APPLICATION_JSON, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                }
            });

//post

    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public byte[] bluetoothpacket(byte[] value) {
        byte[] Lamov = new byte[6];
        if (value.length < 6) {
            if (value.length == 4) {
                Lamov[0] = (byte) 0x30;
                Lamov[1] = (byte) 0x30;
                Lamov[2] = value[0];
                Lamov[3] = value[1];
                Lamov[4] = value[2];
                Lamov[5] = value[3];
            } else if (value.length == 5) {
                Lamov[0] = (byte) 0x30;
                Lamov[1] = value[0];
                Lamov[2] = value[1];
                Lamov[3] = value[2];
                Lamov[4] = value[3];
                Lamov[5] = value[4];
            }
        }

        return Lamov;

    }
}
