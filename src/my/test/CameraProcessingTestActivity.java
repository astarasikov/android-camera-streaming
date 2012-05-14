package my.test;

import my.test.net.http.HttpServer;
import my.test.net.http.MotionJpegStreamer;
import my.test.net.tcp.TcpUnicastClient;
import my.test.net.tcp.TcpUnicastServer;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.VideoView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

public class CameraProcessingTestActivity extends Activity {
    
	CameraServer preview;
	
	protected void startNetwork(final VideoView remoteView,
			String remoteAddr, CameraServer cameraServer)
	{
		ImageSink sink = null;
		TcpUnicastClient src = null;
		
		int port = 45678;
		try {
			sink = new TcpUnicastServer(port, cameraServer.getExecutor());
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
		});
		
		cameraServer.addImageSink(sink);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.camera_menu, menu);
	}
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        VideoView local = (VideoView)findViewById(R.id.view_local);
        SharedPreferences prefs =
        		PreferenceManager.getDefaultSharedPreferences(this);
 
        final String keyRemoteAddr = getString(R.string.key_pref_remote_addr);
        final String remoteAddr = prefs.getString(keyRemoteAddr, "127.0.0.1");
        final VideoView remote = (VideoView)findViewById(R.id.view_remote);
        
        CameraServer.Parameters params =
        		new CameraServer.Parameters(320, 240, false, local);
        
        preview = new CameraServer(params);
        
        new Thread() {
        	@Override
        	public void run() {
            	startNetwork(remote, remoteAddr, preview);        		
        	};
        }.start();
        
        MotionJpegStreamer mjpgStreamer = new MotionJpegStreamer();
        preview.addImageSink(mjpgStreamer);
        
        try {
        	HttpServer srv = new HttpServer(8080);
        	srv.addHandler("video.jpg", mjpgStreamer);
        }
        catch (Exception e) {
        	Log.e("camera processing", "failed to start HTTP server");
        }
                
        ((Button)findViewById(R.id.pref_button)).setOnClickListener(
	        new OnClickListener() {
				@Override
				public void onClick(View v) {
					openContextMenu(findViewById(R.id.main_view));
				}
			});
        
        ((Button)findViewById(R.id.focus_button)).
        	setOnClickListener(focusListener);        
        ((ToggleButton)findViewById(R.id.switch_streaming)).
        	setOnCheckedChangeListener(streamingListener);        
        ((ToggleButton)findViewById(R.id.switch_camera)).
        	setOnCheckedChangeListener(frontCameraListener);
    }
    
    OnClickListener focusListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			preview.focus();
		}
	};
	
	OnCheckedChangeListener streamingListener = new OnCheckedChangeListener()
	{
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked)
		{
			if (isChecked) {
				preview.start();
			}
			else {
				preview.stop();
			}
		}
	};
    
    protected void setUseFrontCamera(boolean enabled) {
		CameraServer.Parameters oldParams = preview.getParameters();
		CameraServer.Parameters params =
				new CameraServer.Parameters(
						oldParams.width,
						oldParams.height,
						enabled,
						oldParams.videoView);
		preview.setParameters(params);
    }
    
    OnCheckedChangeListener frontCameraListener = new OnCheckedChangeListener()
    {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked)
		{
			setUseFrontCamera(isChecked);
		}
	};
}