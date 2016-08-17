package com.example.user.toloyee;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
private TextView tvX, tvY, tvZ;
    private SensorManager sensorManager;
    private Sensor acce;

    private static  final  int X = 0 ;
    private static  final  int Y = 1 ;
    private static  final  int Z = 2 ;

    private static final float MAX_ACCEL = 5;//最大差
    private static final int SHAKE_COUNT =3;//搖晃次數
    private static final int SHAKE_COUNT_TWO = 5;
    private static final int DURATION = 1000;//間隔

    private long stareTime = 0;
    private int count = 0;//跟SHAKE_COUNT比較

    private float[] mGravity = {0.0f, 0.0f, 0.0f};
    private float[] mLinearAccel = {0.0f, 0.0f, 0.0f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvX=(TextView)findViewById(R.id.tvX);
        tvY=(TextView)findViewById(R.id.tvY);
        tvZ=(TextView)findViewById(R.id.tvZ);
        sensorManager = (SensorManager)this.getSystemService(SENSOR_SERVICE);
        acce = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        tvX.setText(Float.toString(event.values[X]));
        tvY.setText(Float.toString(event.values[Y]));
        tvZ.setText(Float.toString(event.values[Z]));


        setAccelValue(event);
        float maxValue = getMaxAccel();
        Log.d("CJCUCSIE", "max accel = " + maxValue);
        if(maxValue > MAX_ACCEL){
            Log.d("CJCUCSIE", "accel  passed");
            long now = System.currentTimeMillis();
            if(stareTime == 0)
                stareTime = now;
            long elapseTime = now - stareTime;//間格時間
            Log.d("CJCUCSIE", "elapse time = " +elapseTime);
            if (elapseTime >DURATION){
                Log.d("CJCUCSIE", "duration not pass");
                stareTime = 0;
                count = 0;

            }else{
                count++;

                if(count > SHAKE_COUNT){
                    tvX.setBackgroundColor(Color.RED);
                    count = 0;
                    stareTime = 0;
                }

            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {




    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, acce);//在背景不讀取
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, acce, SensorManager.SENSOR_DELAY_NORMAL);//讀取&delay time
    }


    public void setAccelValue(SensorEvent event){
        final  float alpha = 0.8f;

        mGravity[X] = alpha * mGravity[X] + (1 - alpha) * event.values[X];
        mGravity[Y] = alpha * mGravity[Y] + (1 - alpha) * event.values[Y];
        mGravity[Z] = alpha * mGravity[Z] + (1 - alpha) * event.values[Z];

        mLinearAccel[X] = event.values[X] - mGravity[X];
        mLinearAccel[Y] = event.values[Y] - mGravity[Y];
        mLinearAccel[Z] = event.values[Z] - mGravity[Z];

    }
    private  float getMaxAccel(){
        float maxValue = mLinearAccel[X];
        if(mLinearAccel[Y] > maxValue)
            maxValue = mLinearAccel[Y];
        if(mLinearAccel[Z] > maxValue)
            maxValue = mLinearAccel[Z];
        return maxValue;


    }
}
