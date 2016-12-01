package a06.csie.cjcu.machine;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by surpr on 2016/11/14.
 */
public class hello_begin extends Activity {

    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hello_begin);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent().setClass(hello_begin.this,MainActivity.class));
            }
        },3000);
    }
}
