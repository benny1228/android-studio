package a06.csie.cjcu.machine;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

/**
 * Created by surpr on 2016/11/14.
 */
public class camera_image extends Activity {

    private ImageButton back_main;
    private Button bt_govideo;
    private EditText ed_videourl;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_image);
        back_main = (ImageButton) findViewById(R.id.back_main);
        back_main.setOnClickListener(new Button.OnClickListener(){ //回主畫面
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(camera_image.this  , MainActivity.class);
                startActivity(intent);
            }
        });

        bt_govideo = (Button) findViewById(R.id.play_video);
        ed_videourl = (EditText) findViewById(R.id.ed_videourl);
        bt_govideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String url =  ed_videourl.getText().toString();
                Intent intent = new Intent(getApplicationContext(), video_view.class);
                intent.putExtra("videoUrl", url);
                startActivity(intent);
            }
        });

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
