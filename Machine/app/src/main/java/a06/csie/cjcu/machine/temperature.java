package a06.csie.cjcu.machine;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

/**
 * Created by surpr on 2016/11/14.
 */
public class temperature extends Activity {
    private ImageButton back_main;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temp_image);
        back_main = (ImageButton) findViewById(R.id.back_main);
        back_main.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(temperature.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
