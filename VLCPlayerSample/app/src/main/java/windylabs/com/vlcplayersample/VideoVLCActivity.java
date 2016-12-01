package windylabs.com.vlcplayersample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;

public class VideoVLCActivity extends Activity implements IVideoPlayer {
    private static final String TAG = VideoVLCActivity.class.getSimpleName();

    // size of the video
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;

    private SurfaceView mSurfaceViewLeft ,mSurfaceViewRight;
    private FrameLayout mSurfaceFrame;
    private SurfaceHolder mSurfaceHolderLeft ,mSurfaceHolderRight;
    private Surface mSurfaceLeft = null;
    private Surface mSurfaceRight = null;

    private LibVLC mLibVLCLeft ,mLibVLCRight;

    private String mMediaUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "VideoVLC -- onCreate -- START ------------");
        setContentView(R.layout.activity_video_vlc);

        mSurfaceViewLeft = (SurfaceView) findViewById(R.id.player_surface_left);
        mSurfaceHolderLeft = mSurfaceViewLeft.getHolder();

        mSurfaceViewRight = (SurfaceView)findViewById(R.id.player_surface_right);
        mSurfaceHolderRight = mSurfaceViewRight.getHolder();


        mSurfaceFrame = (FrameLayout) findViewById(R.id.player_surface_frame);
        mMediaUrl = getIntent().getExtras().getString("videoUrl");

        try {
            mLibVLCLeft = new LibVLC();
            mLibVLCLeft.setAout(mLibVLCLeft.AOUT_AUDIOTRACK);
            mLibVLCLeft.setVout(mLibVLCLeft.VOUT_ANDROID_SURFACE);
            mLibVLCLeft.setHardwareAcceleration(LibVLC.HW_ACCELERATION_FULL);

            mLibVLCLeft.init(getApplicationContext());
        } catch (LibVlcException e){
            Log.e(TAG, e.toString());
        }

        try {
            mLibVLCRight = new LibVLC();
            mLibVLCRight.setAout(mLibVLCRight.AOUT_AUDIOTRACK);
            mLibVLCRight.setVout(mLibVLCRight.VOUT_ANDROID_SURFACE);
            mLibVLCRight.setHardwareAcceleration(LibVLC.HW_ACCELERATION_FULL);

            mLibVLCRight.init(getApplicationContext());
        } catch (LibVlcException e){
            Log.e(TAG, e.toString());
        }

        mSurfaceLeft = mSurfaceHolderLeft.getSurface();
        mSurfaceRight = mSurfaceHolderRight.getSurface();
        mLibVLCLeft.attachSurface(mSurfaceLeft, VideoVLCActivity.this);
        mLibVLCLeft.playMRL(mMediaUrl);

        mLibVLCRight.attachSurface(mSurfaceRight, VideoVLCActivity.this);
        mLibVLCRight.playMRL(mMediaUrl);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // MediaCodec opaque direct rendering should not be used anymore since there is no surface to attach.
        mLibVLCLeft.stop();
        mLibVLCRight.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_video_vlc, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void eventHardwareAccelerationError() {
        Log.e(TAG, "eventHardwareAccelerationError()!");
        return;
    }

    @Override
    public void setSurfaceLayout(final int width, final int height, int visible_width, int visible_height, final int sar_num, int sar_den){
        Log.d(TAG, "setSurfaceSize -- START");
        if (width * height == 0)
            return;

        // store video size
        mVideoHeight = height;
        mVideoWidth = width;
        mVideoVisibleHeight = visible_height;
        mVideoVisibleWidth = visible_width;
        mSarNum = sar_num;
        mSarDen = sar_den;

        Log.d(TAG, "setSurfaceSize -- mMediaUrl: " + mMediaUrl + " mVideoHeight: " + mVideoHeight + " mVideoWidth: " + mVideoWidth + " mVideoVisibleHeight: " + mVideoVisibleHeight + " mVideoVisibleWidth: " + mVideoVisibleWidth + " mSarNum: " + mSarNum + " mSarDen: " + mSarDen);
    }

    @Override
    public int configureSurface(android.view.Surface surface, int i, int i1, int i2){
        return -1;
    }
}
