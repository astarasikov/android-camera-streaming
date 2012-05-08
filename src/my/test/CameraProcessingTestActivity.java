package my.test;

import my.test.net.tcp.TcpUnicastClient;
import my.test.net.tcp.TcpUnicastServer;
import my.test.ui.StartServerActivity;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.VideoView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

public class CameraProcessingTestActivity extends Activity {
    
	CameraServer preview;
	
	public void onPreferencesClick() {
		//startActivity(new Intent(this, CameraPreferences.class));
		startActivityForResult(
				new Intent(this, StartServerActivity.class),
				0);
	}
	
	protected void startNetwork() {
		/*
		ImageSink sink = null;
		TcpUnicastClient src = null;
		
		int port = 45678;
		try {
			sink = new TcpUnicastServer(port, sinkExecutor);
		}
		catch (Exception e) {
			Log.e("xcam", "failed to create sink");
		}
		
		try {
			src = new TcpUnicastClient();
			src.connect(remoteAddr, port);
		}
		catch (Exception e) {
			Log.e("xcam", "failed to create source", e);
		}
		
		src.setOnFrameBitmapCallback(new ImageSource.OnFrameBitmapCallback() {
			@Override
			public void onFrame(Bitmap bitmap) {
				if (bitmap == null) {
					return;
				}
				
				synchronized(CameraServer.class) {
					SurfaceHolder surfaceHolder = remoteView.getHolder();
					Canvas canvas = surfaceHolder.lockCanvas();		
					canvas.drawBitmap(bitmap, 0, 0, null);
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		});	*/	
	}
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        VideoView local = (VideoView)findViewById(R.id.view_local);
        VideoView remote = (VideoView)findViewById(R.id.view_remote);
        
        SharedPreferences prefs =
        		PreferenceManager.getDefaultSharedPreferences(this);
        String keyRemoteAddr = getString(R.string.key_pref_remote_addr);
        String remoteAddr = prefs.getString(keyRemoteAddr, "127.0.0.1");
        
        preview = new CameraServer(local);
        
        ((Button)findViewById(R.id.pref_button)).setOnClickListener(
	        new OnClickListener() {
				@Override
				public void onClick(View v) {
					onPreferencesClick();
				}
			});
        
        ((Button)findViewById(R.id.focus_button)).setOnClickListener(
        	new OnClickListener() {
				@Override
				public void onClick(View v) {
					preview.focus();
				}
        	});
        
        ToggleButton s = (ToggleButton)findViewById(R.id.switch_streaming);
        s.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					preview.start();
				}
				else {
					preview.stop();
				}
			}
		});
        
        s = (ToggleButton)findViewById(R.id.switch_camera);
        s.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				preview.setUseFrontCamera(isChecked);
			}
		});
    }
}