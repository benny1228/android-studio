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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private String push;


    //以下bluetooth
    private Button mbuttonConnenction ,mbuttonSearch ,mbuttonOpenClose;

    private static ScrollView scrollView;

    static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    static final UUID uuid = UUID.fromString(SPP_UUID);
    static final String tag = "BtSPP";

    ArrayList<String> devices = new ArrayList<String>();
    ArrayAdapter<String> devAdapter, adapter1;
    BluetoothAdapter btAdapt;
    BluetoothSocket btSocket;
    InputStream btIn = null;
    OutputStream btOut;
    SppServer sppServer;
    boolean sppConnected = false;
    String devAddr = null;
    Spinner mSpinnerBluetoothSelect;
    private String msg = "";
    private TextView mTextViewMsg;

    private GoogleApiClient client;
    private byte[] x, y, z;
    private byte[] Cmd;




    @SuppressWarnings("deprecation")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//biuetooth
        mbuttonConnenction = (Button)findViewById(R.id.buttonConnection);
        mbuttonOpenClose = (Button)findViewById(R.id.buttonOpenClose);
        mbuttonSearch = (Button)findViewById(R.id.buttonSearch);
        mSpinnerBluetoothSelect = (Spinner) findViewById(R.id.uart_btselect);
        mbuttonConnenction.setOnClickListener(btn_connection_listener);
        mbuttonSearch.setOnClickListener(button_search);
        mbuttonOpenClose.setOnClickListener(button_open_close);

        adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, devices);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerBluetoothSelect.setAdapter(adapter1);

        mSpinnerBluetoothSelect.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                devAddr = ((String) devices.get(position)).split("\\|")[1];
                adapterView.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this,
                        "你選擇藍芽裝置:" + adapterView.getSelectedItem().toString(),
                        Toast.LENGTH_LONG).show();
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                Toast.makeText(MainActivity.this, "你沒有選擇任何藍芽裝置", Toast.LENGTH_LONG).show();
            }
        });

        btAdapt = BluetoothAdapter.getDefaultAdapter();//初始化藍牙
        //用BroadcastReceiver來取得搜索結果
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(searchDevices, intent);//註冊廣播器接收
        //執行ssp服務Thread
        sppServer = new SppServer();
        sppServer.start();

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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

    private BroadcastReceiver searchDevices = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle b = intent.getExtras();
            Object[] lstName = b.keySet().toArray();
            //顯示所有收到的資訊及細節
            for (int i = 0; i < lstName.length; i++) {
                String keyName = lstName[i].toString();
                Log.e(keyName, String.valueOf(b.get(keyName)));
            }
            BluetoothDevice device = null;
            //搜尋設備時，取得設備的mac位址
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String str = device.getName() + "|" + device.getAddress();
                if (devices.indexOf(str) == -1)//防止重複增加
                    devices.add(str);//獲得設備名稱和mac位址
                adapter1.notifyDataSetChanged();
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch ((device.getBondState())) {
                    case BluetoothDevice.BOND_BONDING:
                        Log.d(tag, "正在配對...");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.d(tag, "完成配對...");
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Log.d(tag, "取消配對...");
                    default:
                        break;
                }
            }
        }
    };
    /*藍牙連線*/
    private Button.OnClickListener btn_connection_listener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (sppConnected || devAddr == null)
                return;
            try {
                //建立ssp的RfcommSocket,開始連接...
                btSocket = btAdapt.getRemoteDevice(devAddr).createRfcommSocketToServiceRecord(uuid);
                btSocket.connect();
                Log.d(tag, "BT_Socket connect");
                synchronized (MainActivity.this) {
                    if (sppConnected)
                        return;
                    btServerSocket.close();
                    btIn = btSocket.getInputStream();//接收外部資料
                    btOut = btSocket.getOutputStream();//將資料外傳
                    conected();
                }
                Toast.makeText(MainActivity.this, "藍芽裝置已開啟:" + devAddr, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                sppConnected = false;
                try {
                    btSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                btSocket = null;
                Toast.makeText(MainActivity.this, "連接阜異常:" + devAddr, Toast.LENGTH_LONG).show();
            }
        }
    };
    private BluetoothServerSocket btServerSocket;

    private class SppServer extends Thread {
        public SppServer() {
            try {
                btServerSocket = btAdapt.listenUsingRfcommWithServiceRecord("SPP", uuid);
            } catch (IOException e) {
                e.printStackTrace();
                btServerSocket = null;
            }
        }

        public void run() {
            BluetoothSocket bs;
            if (btServerSocket == null) {
                Log.e(tag, "ServerSocket null");
                return;
            }
            try {
                bs = btServerSocket.accept();
                synchronized (MainActivity.this) {
                    if (sppConnected)
                        return;
                    Log.i(tag, "Devices Name:" + bs.getRemoteDevice().getName());
                    btIn = bs.getInputStream();
                    btOut = bs.getOutputStream();
                    conected();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(tag, "ServerSocket accept failed");
            }
            Log.i(tag, "End Bluetooth SPP Server");
        }
        public void cancel() {
            if (btServerSocket == null)
                return;
            try {
                btServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(tag, "close ServerSocket failed");
            }
        }
    }
    private void conected() {
        sppConnected = true;
        new SppReceiver(btIn).start();
        mSpinnerBluetoothSelect.setClickable(false);
        sppServer = null;
        Log.e(tag, "conected");
    }
    private void disconnect() {
        mSpinnerBluetoothSelect.setClickable(true);
        sppConnected = false;
        btIn = null;
        btOut = null;
        sppServer = new SppServer();
        sppServer.start();
        Log.e(tag, "disconnect");
    }


    private class SppReceiver extends Thread {
        private InputStream input = null;

        public SppReceiver(InputStream in) {
            input = in;
            Log.i(tag, "SppReceiver");
        }
        /*接收spp訊息*/
        public void run() {
            byte[] data = new byte[1024];
            int length = 0;
            if (input == null) {
                Log.d(tag, "InputStream null");
                return;
            }
            while (true) {
                try {
                    length = input.read(data);
                    Log.i(tag, "SPP receiver");
                    if (length > 0) {
                        msg = new String(data, 0, length, "ASCII") + "\n";
                        btHandler.sendEmptyMessage(0);

                    }
                } catch (IOException e) {
                    Log.e(tag, "SppReceiver_disconnect");

                }
            }
        }
    }
    Handler btHandler = new Handler() {
        public void handleMessage(Message m) {
            mTextViewMsg.append(msg);
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);

        }
    };
    private Button.OnClickListener button_search = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            btAdapt.cancelDiscovery();
            btAdapt.startDiscovery();
        }
    };
    private Button.OnClickListener button_open_close = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (btAdapt.isEnabled()) {
                btAdapt.disable();
            } else {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(intent);
            }
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sppServer != null)
            sppServer.cancel();
        this.unregisterReceiver(searchDevices);
        if (btIn != null) {
            try {
                btSocket.close();
                btServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Process.killProcess(Process.myPid());
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

        byte[]a = String.format("%.2f", event.values[0]).getBytes(StandardCharsets.US_ASCII);
        byte[]b = String.format("%.2f", event.values[1]).getBytes(StandardCharsets.US_ASCII);
        byte[]c = String.format("%.2f", event.values[2]).getBytes(StandardCharsets.US_ASCII);

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

        servo[0] = (byte) 0xB0;
        servo[1] = (byte) 0x59;
        if (Cmd[9]==48){
            if (Cmd[10]==48){
                    if (Cmd[11]==48){
                        if (Cmd[13]==48){
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x30;
                            servo[4]=(byte)0x30;
                        }
                    }
            }
        }else if (Cmd[9]==45) {
            if (Cmd[10] == 48) {
                if (Cmd[11] == 48) {
                    if (Cmd[13] == 48) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x31;
                    }
                } else if (Cmd[11] == 49) {
                    if (Cmd[13] == 48) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x33;
                    }
                } else if (Cmd[11] == 50) {
                    if (Cmd[13] == 48) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x35;
                    }
                } else if (Cmd[11] == 51) {
                    if (Cmd[13] == 48) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x37;
                    }
                } else if (Cmd[11] == 52) {
                    if (Cmd[13] == 48) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x39;
                    }
                } else if (Cmd[11] == 53) {
                    if (Cmd[13] == 48) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x31;
                    }
                } else if (Cmd[11] == 54) {
                    if (Cmd[13] == 48) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x33;
                    }
                } else if (Cmd[11] == 55) {
                    if (Cmd[13] == 48) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x35;
                    }
                } else if (Cmd[11] == 56) {
                    if (Cmd[13] == 48) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x37;
                    }
                } else if (Cmd[11] == 57) {
                    if (Cmd[13] == 48) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x39;
                    }
                }
            }else if (Cmd[10]==49){
                if (Cmd[11]==48){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==49){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x33;
                    }
                }else if (Cmd[11]==50){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x35;
                    }
                }else if (Cmd[11]==51){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x37;
                    }
                }else if (Cmd[11]==52){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x39;
                    }
                }else if (Cmd[11]==53){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x33;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x33;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==54){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x33;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x33;
                        servo[4] = (byte) 0x33;
                    }
                }else if (Cmd[11]==55){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x33;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x33;
                        servo[4] = (byte) 0x35;
                    }
                }else if (Cmd[11]==56){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x33;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x33;
                        servo[4] = (byte) 0x37;
                    }
                }else if (Cmd[11]==57){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x33;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x33;
                        servo[4] = (byte) 0x39;
                    }
                }
            }else if (Cmd[10]==50){
                if (Cmd[11]==48){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x34;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x34;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==49){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x34;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x34;
                        servo[4] = (byte) 0x33;
                    }
                }else if (Cmd[11]==50){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x34;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x34;
                        servo[4] = (byte) 0x35;
                    }
                }else if (Cmd[11]==51){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x34;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x34;
                        servo[4] = (byte) 0x37;
                    }
                }else if (Cmd[11]==52){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x34;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x34;
                        servo[4] = (byte) 0x39;
                    }
                }else if (Cmd[11]==53){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x35;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x35;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==54){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x35;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x35;
                        servo[4] = (byte) 0x33;
                    }
                }else if (Cmd[11]==55){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x35;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x35;
                        servo[4] = (byte) 0x35;
                    }
                }else if (Cmd[11]==56){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x35;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x35;
                        servo[4] = (byte) 0x37;
                    }
                }else if (Cmd[11]==57){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x35;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x35;
                        servo[4] = (byte) 0x39;
                    }
                }
            }else if (Cmd[10]==51){
                if (Cmd[11]==48){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x36;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x36;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==49){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x36;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x36;
                        servo[4] = (byte) 0x33;
                    }
                }else if (Cmd[11]==50){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x36;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x36;
                        servo[4] = (byte) 0x35;
                    }
                }else if (Cmd[11]==51){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x36;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x36;
                        servo[4] = (byte) 0x37;
                    }
                }else if (Cmd[11]==52){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x36;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x36;
                        servo[4] = (byte) 0x39;
                    }
                }else if (Cmd[11]==53){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x37;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x37;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==54){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x37;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x37;
                        servo[4] = (byte) 0x33;
                    }
                }else if (Cmd[11]==55){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x37;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x37;
                        servo[4] = (byte) 0x35;
                    }
                }else if (Cmd[11]==56){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x37;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x37;
                        servo[4] = (byte) 0x37;
                    }
                }else if (Cmd[11]==57){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x37;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x37;
                        servo[4] = (byte) 0x39;
                    }
                }
            }else if (Cmd[10]==52){
                if (Cmd[11]==48){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x38;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x38;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==49){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x38;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x38;
                        servo[4] = (byte) 0x33;
                    }
                }else if (Cmd[11]==50){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x38;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x38;
                        servo[4] = (byte) 0x35;
                    }
                }else if (Cmd[11]==51){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x38;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x38;
                        servo[4] = (byte) 0x37;
                    }
                }else if (Cmd[11]==52){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x38;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x38;
                        servo[4] = (byte) 0x39;
                    }
                }else if (Cmd[11]==53){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x39;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x39;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==54){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x39;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x39;
                        servo[4] = (byte) 0x33;
                    }
                }else if (Cmd[11]==55){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x39;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x39;
                        servo[4] = (byte) 0x35;
                    }
                }else if (Cmd[11]==56){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x39;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x39;
                        servo[4] = (byte) 0x37;
                    }
                }else if (Cmd[11]==57){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x39;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x30;
                        servo[3] = (byte) 0x39;
                        servo[4] = (byte) 0x39;
                    }
                }
            }else if (Cmd[10]==53){
                if (Cmd[11]==48){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==49){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x33;
                    }
                }else if (Cmd[11]==50){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x35;
                    }
                }else if (Cmd[11]==51){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x37;
                    }
                }else if (Cmd[11]==52){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x30;
                        servo[4] = (byte) 0x39;
                    }
                }else if (Cmd[11]==53){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==54){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x33;
                    }
                }else if (Cmd[11]==55){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x34;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x35;
                    }
                }else if (Cmd[11]==56){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x36;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x37;
                    }
                }else if (Cmd[11]==57){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x38;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x31;
                        servo[4] = (byte) 0x39;
                    }
                }
            }else if (Cmd[10]==54){
                if (Cmd[11]==48){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x30;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x31;
                    }
                }else if (Cmd[11]==49){
                    if (Cmd[13]==48){
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x32;
                    } else if (Cmd[13] == 53) {
                        servo[2] = (byte) 0x31;
                        servo[3] = (byte) 0x32;
                        servo[4] = (byte) 0x33;
                    }else if (Cmd[11]==50){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x32;
                            servo[4] = (byte) 0x34;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x32;
                            servo[4] = (byte) 0x35;
                        }
                    }else if (Cmd[11]==51){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x32;
                            servo[4] = (byte) 0x36;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x32;
                            servo[4] = (byte) 0x37;
                        }
                    }else if (Cmd[11]==52){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x32;
                            servo[4] = (byte) 0x38;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x32;
                            servo[4] = (byte) 0x39;
                        }
                    }else if (Cmd[11]==53){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x33;
                            servo[4] = (byte) 0x30;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x33;
                            servo[4] = (byte) 0x31;
                        }
                    }else if (Cmd[11]==54){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x33;
                            servo[4] = (byte) 0x32;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x33;
                            servo[4] = (byte) 0x33;
                        }
                    }else if (Cmd[11]==55){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x33;
                            servo[4] = (byte) 0x34;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x33;
                            servo[4] = (byte) 0x35;
                        }
                    }else if (Cmd[11]==56){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x33;
                            servo[4] = (byte) 0x36;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x33;
                            servo[4] = (byte) 0x37;
                        }
                    }else if (Cmd[11]==57){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x33;
                            servo[4] = (byte) 0x38;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x33;
                            servo[4] = (byte) 0x39;
                        }
                    }
                }else if (Cmd[10]==55) {
                    if (Cmd[11] == 48) {
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x34;
                            servo[4] = (byte) 0x30;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x34;
                            servo[4] = (byte) 0x31;
                        }
                    } else if (Cmd[11] == 49) {
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x34;
                            servo[4] = (byte) 0x32;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x34;
                            servo[4] = (byte) 0x33;
                        }
                    }else if (Cmd[11]==50){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x34;
                            servo[4] = (byte) 0x34;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x34;
                            servo[4] = (byte) 0x35;
                        }
                    }else if (Cmd[11]==51){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x34;
                            servo[4] = (byte) 0x36;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x34;
                            servo[4] = (byte) 0x37;
                        }
                    }else if (Cmd[11]==52){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x34;
                            servo[4] = (byte) 0x38;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x34;
                            servo[4] = (byte) 0x39;
                        }
                    }else if (Cmd[11]==53){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x35;
                            servo[4] = (byte) 0x30;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x35;
                            servo[4] = (byte) 0x31;
                        }
                    }else if (Cmd[11]==54){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x35;
                            servo[4] = (byte) 0x32;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x35;
                            servo[4] = (byte) 0x33;
                        }
                    }else if (Cmd[11]==55){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x35;
                            servo[4] = (byte) 0x34;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x35;
                            servo[4] = (byte) 0x35;
                        }
                    }else if (Cmd[11]==56){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x35;
                            servo[4] = (byte) 0x36;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x35;
                            servo[4] = (byte) 0x37;
                        }
                    }else if (Cmd[11]==57){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x35;
                            servo[4] = (byte) 0x38;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x35;
                            servo[4] = (byte) 0x39;
                        }
                    }
                }else if (Cmd[10]==56) {
                    if (Cmd[11] == 48) {
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x36;
                            servo[4] = (byte) 0x30;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x36;
                            servo[4] = (byte) 0x31;
                        }
                    } else if (Cmd[11] == 49) {
                            if (Cmd[13] == 48) {
                                servo[2] = (byte) 0x31;
                                servo[3] = (byte) 0x36;
                                servo[4] = (byte) 0x32;
                            } else if (Cmd[13] == 53) {
                                servo[2] = (byte) 0x31;
                                servo[3] = (byte) 0x36;
                                servo[4] = (byte) 0x33;
                            }
                    }else if (Cmd[11]==50){
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x36;
                            servo[4] = (byte) 0x34;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x36;
                            servo[4] = (byte) 0x35;
                        }
                    }else if (Cmd[11]==51){
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x36;
                            servo[4] = (byte) 0x36;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x36;
                            servo[4] = (byte) 0x37;
                        }
                    }else if (Cmd[11]==52){
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x36;
                            servo[4] = (byte) 0x38;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x36;
                            servo[4] = (byte) 0x39;
                        }
                    }else if (Cmd[11]==53){
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x37;
                            servo[4] = (byte) 0x30;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x37;
                            servo[4] = (byte) 0x31;
                        }
                    }else if (Cmd[11]==54) {
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x37;
                            servo[4] = (byte) 0x32;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x37;
                            servo[4] = (byte) 0x33;
                        }
                    }else if (Cmd[11]==55){
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x37;
                            servo[4] = (byte) 0x34;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x37;
                            servo[4] = (byte) 0x35;
                        }
                    }else if (Cmd[11]==56){
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x37;
                            servo[4] = (byte) 0x36;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x37;
                            servo[4] = (byte) 0x37;
                        }
                    }else if (Cmd[11]==57){
                        if (Cmd[13] == 48) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x37;
                            servo[4] = (byte) 0x38;
                        } else if (Cmd[13] == 53) {
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x37;
                            servo[4] = (byte) 0x39;
                        }
                    }
                }else if (Cmd[10]==57){
                    if (Cmd[11]==48){
                        if (Cmd[13]==48){
                            servo[2] = (byte) 0x31;
                            servo[3] = (byte) 0x38;
                            servo[4] = (byte) 0x30;
                        }
                    }
                }
            }
        }
        Log.e("view push","push:"+push);


        try {
            if (sppConnected == true){
                btOut.write(servo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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
