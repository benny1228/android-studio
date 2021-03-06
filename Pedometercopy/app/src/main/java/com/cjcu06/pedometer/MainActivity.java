package com.cjcu06.pedometer;

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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

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
    Button mButtonConnection, mButtonSearch, mButtonOpenClose;
    String devAddr = null;
    Spinner mSpinnerBluetoothSelect;
    private String msg = "";
    private TextView mTextViewMsg;


    private SensorManager sensorManager;
    private Sensor mSensorAccelerometer;
    private TextView mTextViewStep;
    private Button mButtonStart;
    private int step = 0;   //步數
    private double oriValue = 0;  //原始值
    private double lstValue = 0;  //上次的值
    private double curValue = 0;  //現在值
    private boolean motiveState = true;   //是否是搖動狀態
    private boolean processState = false;   //標記現在是否已經在計步

    private TextView mTextViewX, mTextViewY, mTextViewZ;
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    private static final float MAX_ACCEL = 5;//最大差
    private static final int SHAKE_COUNT = 3;//搖晃次數
    private static final int SHAKE_COUNT_TWO = 5;
    private static final int DURATION = 1000;//間隔
    private long stareTime = 0;
    private int count = 0;//跟SHAKE_COUNT比較
    private float[] mGravity = {0.0f, 0.0f, 0.0f};
    private float[] mLinearAccel = {0.0f, 0.0f, 0.0f};

    private byte[] Cmd;
    private byte[] x, y, z;



    private Sensor gyroSensor = null;
    private TextView mTextViewGSensorX;
    private TextView mTextViewGSensorY;
    private TextView mTextViewGSensorZ;
    private byte[] servo=new byte[5];
    private Button button;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private float[] angle = new float[3];
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @SuppressWarnings("deprecation")


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewGSensorX = (TextView) findViewById(R.id.vx);
        mTextViewGSensorY = (TextView) findViewById(R.id.vy);



        mButtonConnection = (Button) findViewById(R.id.btn_connection);
        mButtonSearch = (Button) findViewById(R.id.btn_search);
        mButtonOpenClose = (Button) findViewById(R.id.btn_openclose);
        mSpinnerBluetoothSelect = (Spinner) findViewById(R.id.uart_btselect);
        mTextViewMsg = (TextView) findViewById(R.id.tv_step);

        mButtonSearch.setOnClickListener(btn_search_listener);
        mButtonConnection.setOnClickListener(btn_connection_listener);
        mButtonOpenClose.setOnClickListener(btn_openclose_listener);
        mTextViewMsg = (TextView) findViewById(R.id.msgtx);

        mTextViewX = (TextView) findViewById(R.id.tvX);
        mTextViewY = (TextView) findViewById(R.id.tvY);
        mTextViewZ = (TextView) findViewById(R.id.tvZ);

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


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//傳感器設定
        mSensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//呼叫加速度傳感器
        sensorManager.registerListener((SensorEventListener) this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
        bindViews();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.cjcu06.pedometer/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

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

        }
    };

    private Button.OnClickListener btn_search_listener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            btAdapt.cancelDiscovery();
            btAdapt.startDiscovery();
        }
    };

    private Button.OnClickListener btn_openclose_listener = new Button.OnClickListener() {
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

    private void bindViews() {
        mTextViewStep = (TextView) findViewById(R.id.tv_step);
        mButtonStart = (Button) findViewById(R.id.btn_start);
        mButtonStart.setOnClickListener((View.OnClickListener) this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        double range = 1;   //設應一個精度範圍
        float[] value = event.values;
        curValue = magnitude(value[0], value[1], value[2]);   //计算当前的模

        //向上加速的状态

        if (motiveState == true) {
            if (curValue >= lstValue) lstValue = curValue;
            else {
                //检测到一次峰值
                if (Math.abs(curValue - lstValue) > range) {
                    oriValue = curValue;
                    motiveState = false;
                }
            }
        }
        //向下加速的状态

        if (motiveState == false) {
            if (curValue <= lstValue) lstValue = curValue;
            else {
                if (Math.abs(curValue - lstValue) > range) {
                    //检测到一次峰值
                    oriValue = curValue;
                    if (processState == true) {
                        step++;  //步数 + 1
                        if (processState == true) {
                            mTextViewStep.setText(step + "");    //读数更新

                        }
                    }
                    motiveState = true;
                }
            }
            mTextViewMsg.setText("a");
        }
        if (Math.abs(curValue - lstValue) < range && Math.abs(curValue - lstValue) > 0) {
            mTextViewMsg.setText("e");
        }

        mTextViewX.setText(String.format("%.2f", event.values[X]));
        mTextViewY.setText(String.format("%.2f", event.values[Y]));
        mTextViewZ.setText(String.format("%.2f", event.values[Z]));

        /*if(event.values[X]>0){
            mTextViewX.setText("+"+mTextViewX.getText().toString());
        }
        if(event.values[Y]>0){
            mTextViewY.setText("+"+mTextViewY.getText().toString());
        }
        if(event.values[Z]>0){
            mTextViewZ.setText("+"+mTextViewZ.getText().toString());
        }*/

        byte[] a = String.format("%.2f", event.values[X]).getBytes(StandardCharsets.US_ASCII);
        bytesToHex(a);

        byte[] b = String.format("%.2f", event.values[Y]).getBytes(StandardCharsets.US_ASCII);

        byte[] c = String.format("%.2f", event.values[Z]).getBytes(StandardCharsets.US_ASCII);

        Log.e("view x", "x:" + bytesToHex(a));
        Log.e("view a", "a:" + a);
        Log.e("View a", "a[0]" + a[0]);
        Log.e("View a", "a[1]" + a[1]);
        Log.e("View a", "a[2]" + a[2]);
        Log.e("View a", "a[3]" + a[3]);


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

        servo[0]=(byte)0xA0;
        if(Cmd[16]==45){
            if(Cmd[17]==48){
                if (Cmd[18]==48){
                    if (Cmd[20]==48){
                        if(Cmd[21]==48){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==49) {
                        if (Cmd[21] == 49) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x31;
                        }
                    }else if(Cmd[20]==50){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x32;
                        }
                    }else if(Cmd[20]==51){
                        if(Cmd[21]==51){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x39;
                        }
                    }

                }else if (Cmd[18]==49){
                    if (Cmd[20]==49){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x38;
                        }
                    }
                }else if(Cmd[18]==50){
                    if (Cmd[20]==48){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[21]==57){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x37;
                        }
                    }
                }else if (Cmd[18]==51){
                    if (Cmd[20]==48){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x36;
                        }
                    }
                }else if (Cmd[18]==52){
                    if (Cmd[20]==48){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==50) {
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x35;

                        }
                    }
                }else if (Cmd[18]==53){
                    if (Cmd[20]==48){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x34;
                        }
                    }
                }else if (Cmd[18]==54){
                    if (Cmd[20]==48){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==51) {
                            servo[1] = (byte) 0x31;
                            servo[2] = (byte) 0x35;
                            servo[3] = (byte) 0x33;
                        }
                    }
                }else if (Cmd[18]==55){
                    if (Cmd[20]==48){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x32;
                        }
                    }
                }else if (Cmd[18]==56){
                    if (Cmd[20]==48){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x31;
                        }
                    }
                }else if (Cmd[18]==57){
                    if (Cmd[20]==48){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x31;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x30;
                        }
                    }
                }
            }
        }else if (Cmd[16]==48){
            if (Cmd[17]==48){
                if (Cmd[18]==48){
                    if (Cmd[20]==48){
                        if(Cmd[21]==48){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x39;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==49) {
                        if (Cmd[21] == 49) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x39;
                        }
                    }else if(Cmd[20]==50){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x38;
                        }
                    }else if(Cmd[20]==51){
                        if(Cmd[21]==51){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x31;
                        }
                    }

                }else if (Cmd[18]==49){
                    if (Cmd[20]==49){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x38;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x33;
                            }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x32;
                        }
                    }
                }else if(Cmd[18]==50){
                    if (Cmd[20]==48){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x37;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[21]==57){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x33;
                        }
                    }
                }else if (Cmd[18]==51){
                    if (Cmd[20]==48){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x36;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x34;
                        }
                    }
                }else if (Cmd[18]==52){
                    if (Cmd[20]==48){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x35;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==50) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==46){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x35;
                        }
                    }
                }else if (Cmd[18]==53){
                    if (Cmd[20]==48){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x34;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x36;
                        }
                    }
                }else if (Cmd[18]==54){
                    if (Cmd[20]==48){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x33;
                            servo[2]=(byte)0x33;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x33;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x38;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==51){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x37;
                        }
                    }
                }else if (Cmd[18]==55){
                    if (Cmd[20]==48){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==55){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x32;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x39;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==50){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x38;
                        }
                    }
                }else if (Cmd[18]==56){
                    if (Cmd[20]==48){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x37;
                        }
                    }else if (Cmd[20]==49){
                        if (Cmd[21]==52){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x36;
                        }
                    }else if (Cmd[20]==50){
                        if (Cmd[21]==53){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x35;
                        }
                    }else if (Cmd[20]==51){
                        if (Cmd[21]==54){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x34;
                        }
                    }else if (Cmd[20]==52){
                        if (Cmd[21]==55){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x33;
                        }
                    }else if (Cmd[20]==53){
                        if (Cmd[21]==56){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x32;
                        }
                    }else if (Cmd[20]==54){
                        if (Cmd[21]==57){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x31;
                        }
                    }else if (Cmd[20]==56){
                        if (Cmd[21]==48){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x31;
                            servo[3]=(byte)0x30;
                        }
                    }else if (Cmd[20]==57){
                        if (Cmd[21]==49){
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x39;
                        }
                    }
                }else if (Cmd[18]==57) {
                    if (Cmd[20] == 48) {
                        if (Cmd[21] == 50) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x38;
                        }
                    } else if (Cmd[20] == 49) {
                        if (Cmd[21] == 51) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x37;
                        }
                    } else if (Cmd[20] == 50) {
                        if (Cmd[21] == 52) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x36;
                        }
                    } else if (Cmd[20] == 51) {
                        if (Cmd[21] == 53) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x35;
                        }
                    } else if (Cmd[20] == 52) {
                        if (Cmd[21] == 54) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x34;
                        }
                    } else if (Cmd[20] == 53) {
                        if (Cmd[21] == 55) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x33;
                        }
                    } else if (Cmd[20] == 54) {
                        if (Cmd[21] == 56) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x32;
                        }
                    } else if (Cmd[20] == 55) {
                        if (Cmd[21] == 57) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x31;
                        }
                    } else if (Cmd[20] == 57) {
                        if (Cmd[21] == 48) {
                            servo[1]=(byte)0x30;
                            servo[2]=(byte)0x30;
                            servo[3]=(byte)0x30;
                        }
                    }
                }
            }
        }
        servo[4]=(byte)0xF0;

        Log.e("Servo", "servo:" + servo[0]);
        Log.e("Servo", "servo:" + servo[1]);
        Log.e("Servo", "servo:" + servo[2]);
       /* mTextViewX.setText(Float.toString(event.values[X]));
        mTextViewY.setText(Float.toString(event.values[Y]));
        mTextViewZ.setText(Float.toString(event.values[Z]));*/

        try {
            if (sppConnected == true) {

                /*mTextViewMsg.append(mTextViewMsg.getText().toString() + "\n");
                btOut.write(mTextViewMsg.getText().toString().getBytes());
                mTextViewMsg.setText("");*/
                btOut.write(servo);  //將值藉由藍芽傳出

                // btOut.write(mTextViewY.getText().toString().getBytes());  //將值藉由藍芽傳出

                //  btOut.write(mTextViewZ.getText().toString().getBytes());  //將值藉由藍芽傳出

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        setAccelValue(event);
        float maxValue = getMaxAccel();
        Log.d("CJCUCSIE", "max accel = " + maxValue);
        if (maxValue > MAX_ACCEL) {
            Log.d("CJCUCSIE", "accel  passed");
            long now = System.currentTimeMillis();
            if (stareTime == 0)
                stareTime = now;
            long elapseTime = now - stareTime;//間格時間
            Log.d("CJCUCSIE", "elapse time = " + elapseTime);
            if (elapseTime > DURATION) {
                Log.d("CJCUCSIE", "duration not pass");
                stareTime = 0;
                count = 0;

            } else {
                count++;

            }

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onClick(View v) {
        step = 0;
        mTextViewStep.setText("0");
        if (processState == true) {
            mButtonStart.setText("開始");
            processState = false;
        } else {
            mButtonStart.setText("停止");
            processState = true;
        }
    }

    //向量求模
    public double magnitude(float x, float y, float z) {
        double magnitude = 0;
        magnitude = Math.sqrt(x * x + y * y + z * z);
        return magnitude;
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, mSensorAccelerometer);//在背景不讀取
    }

    protected void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.cjcu06.pedometer/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);//讀取&delay time
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener((SensorEventListener) this);
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

    public void setAccelValue(SensorEvent event) {
        final float alpha = 0.8f;

        mGravity[X] = alpha * mGravity[X] + (1 - alpha) * event.values[X];
        mGravity[Y] = alpha * mGravity[Y] + (1 - alpha) * event.values[Y];
        mGravity[Z] = alpha * mGravity[Z] + (1 - alpha) * event.values[Z];

        mLinearAccel[X] = event.values[X] - mGravity[X];
        mLinearAccel[Y] = event.values[Y] - mGravity[Y];
        mLinearAccel[Z] = event.values[Z] - mGravity[Z];

    }

    private float getMaxAccel() {
        float maxValue = mLinearAccel[X];
        if (mLinearAccel[Y] > maxValue)
            maxValue = mLinearAccel[Y];
        if (mLinearAccel[Z] > maxValue)
            maxValue = mLinearAccel[Z];
        return maxValue;


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
