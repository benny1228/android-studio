package com.cjcu06.gsensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

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

    @SuppressWarnings("deprecation")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        vX.setText("Orientation X: " + event.values[0]);
        vY.setText("Orientation Y: " + event.values[1]);
        vZ.setText("Orientation Z: " + event.values[2]);


    }
}
