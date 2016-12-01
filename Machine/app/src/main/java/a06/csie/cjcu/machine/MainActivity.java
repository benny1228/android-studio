package a06.csie.cjcu.machine;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private ImageButton ib_image;
    private ImageButton ib_cardboard;
    private ImageButton ib_sensors;
    private ImageButton ib_about;
    private TextView tv_display;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ib_image = (ImageButton)findViewById(R.id.ib_image);
        ib_cardboard = (ImageButton)findViewById(R.id.ib_cardboard);
        ib_sensors = (ImageButton)findViewById(R.id.ib_sensors);
        ib_about = (ImageButton)findViewById(R.id.ib_about);
        tv_display = (TextView)findViewById(R.id.tv_display);

        ib_image.setOnClickListener(new View.OnClickListener() {  //房子介面轉換
            @Override
            public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this, camera_image.class);
                    startActivity(intent);
            }
        });
        ib_image.setOnLongClickListener(new View.OnLongClickListener() { //房子介紹
            @Override
            public boolean onLongClick(View view) {
                tv_display.setText("攝像頭影像.藉由控制手機搖晃,來選擇你想觀看的角度");
                return true;
            }
        });
        ib_image.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {  //手起來的時候
                    tv_display.setText("長按圖示說明");
                }
                return false;
            }
        });

        ib_cardboard.setOnClickListener(new View.OnClickListener() { //眼鏡介面轉換
            @Override
            public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this, cardboard_image.class);
                    startActivity(intent);
            }
        });
        ib_cardboard.setOnLongClickListener(new View.OnLongClickListener() { //眼鏡介紹
            @Override
            public boolean onLongClick(View view) {
                tv_display.setText("將影像做切割及配合cardboard,達到身歷其境的感受");
                return true;
            }
        });
        ib_cardboard.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {  //手起來的時候
                    tv_display.setText("長按圖示說明");
                }
                return false;
            }
        });

        ib_sensors.setOnClickListener(new View.OnClickListener() { //溫度計介面轉換
            @Override
            public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this, temperature.class);
                    startActivity(intent);
            }
        });
        ib_sensors.setOnLongClickListener(new View.OnLongClickListener() { //溫度計介紹
            @Override
            public boolean onLongClick(View view) {
                tv_display.setText("基座所搭載的感測器.藉由物聯網記錄成圖表,供使用者觀看分析");
                return true;
            }
        });
        ib_sensors.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {  //手起來的時候
                    tv_display.setText("長按圖示說明");
                }
                return false;
            }
        });

        ib_about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAlertDialog1Event();
            }
        });
    }
    private void setAlertDialog1Event() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.Developer)
                .setMessage(R.string.Group_members)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), R.string.welcome, Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }
}
