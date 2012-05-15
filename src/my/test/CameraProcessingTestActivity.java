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
	static class VideoViewSink implements ImageSink {
		VideoView videoView;
		
		public VideoViewSink(VideoView videoView) {
			this.videoView = videoView;
		}
		
		@Override
		public void send(Bitmap bitmap) throws Exception {
			SurfaceHolder surfaceHolder = videoView.getHolder();
			Canvas canvas = surfaceHolder.lockCanvas();		
			canvas.drawBitmap(bitmap, 0, 0, null);
			surfaceHolder.unlockCanvasAndPost(canvas);
		}

		@Override
		public void teardown() {			
		}
	}
	
	static public class DrawBitmapCallback
		implements ImageSource.OnFrameBitmapCallback
	{
		VideoView videoView;
		
		public DrawBitmapCallback(VideoView videoView) {
			this.videoView = videoView;
		}
	
		@Override
		public void onFrame(Bitmap bitmap) {
			if (bitmap == null) {
				return;
			}
			
			synchronized(ImageGraph.class) {
				SurfaceHolder surfaceHolder = videoView.getHolder();
				Canvas canvas = surfaceHolder.lockCanvas();		
				canvas.drawBitmap(bitmap, 0, 0, null);
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
		
	}
	
	final static String LOG_TAG =
			CameraProcessingTestActivity.class.getSimpleName();
	
	CameraProcessingTestActivity this_activity = this;
	ImageGraph mImageGraph;
	
	SharedPreferences mSharedPreferences;
	
	protected String stringPreference(int keyId, String defaultValue) {
		String key = getString(keyId);
		return mSharedPreferences.getString(key, defaultValue);
	}
	
	protected int intPreference(int keyId, int defaultValue) {
		String key = getString(keyId);
		return mSharedPreferences.getInt(key, defaultValue);
	}
	
	protected boolean booleanPreference(int keyId, boolean defaultValue) {
		String key = getString(keyId);
		return mSharedPreferences.getBoolean(key, defaultValue);
	}
	
	protected void startNetworkServer() throws Exception {
        boolean startServer =
        		booleanPreference(R.string.key_pref_server, true);
        if (!startServer) {
        	return;
        }
        int localPort =
        		intPreference(R.string.key_pref_local_port, 8082);
		
        
		ImageSink tcpServer = null;
		tcpServer = new TcpUnicastServer(localPort,
				mImageGraph.getExecutor());
		mImageGraph.addImageSink(tcpServer);
	}
	
	protected void startNetworkClient(VideoView videoView) throws Exception {
        boolean startClient =
        		booleanPreference(R.string.key_pref_client, true);
        if (!startClient) {
        	return;
        }
		TcpUnicastClient tcpClient = null;
        String remoteAddr =
        		stringPreference(R.string.key_pref_remote_addr,
        				"127.0.0.1");
		int remotePort =
        		intPreference(R.string.key_pref_remote_port, 8082);
		
		tcpClient = new TcpUnicastClient();
		tcpClient.connect(remoteAddr, remotePort);
							
		DrawBitmapCallback callback =
				new DrawBitmapCallback(videoView);
		tcpClient.setOnFrameBitmapCallback(callback);
	}
	
	protected void startNetwork(final VideoView remoteView,
			final ImageGraph cameraServer)
	{
		new Thread() {
			public void run() {
				try {
					startNetworkServer();
					startNetworkClient(remoteView);
				}
				catch (Exception e) {
					Log.e(LOG_TAG, "Failed to start streaming", e);
				}
			}
		}.start();
	}
		
	protected void startHttp() {
        try {
            MotionJpegStreamer mjpgStreamer = new MotionJpegStreamer();
            mImageGraph.addImageSink(mjpgStreamer);
        	HttpServer srv = new HttpServer(8080);
        	srv.addHandler("video.jpg", mjpgStreamer);
        }
        catch (Exception e) {
        	Log.e(LOG_TAG, "failed to start HTTP server", e);
        }
	}
	
	protected void startLocalPreview() {
        VideoView local = (VideoView)findViewById(R.id.view_local); 
        VideoViewSink videoViewSink = new VideoViewSink(local);
        mImageGraph.addImageSink(videoViewSink);
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
        
        mSharedPreferences = 
        		PreferenceManager.getDefaultSharedPreferences(this);
        VideoView remote = (VideoView)findViewById(R.id.view_remote);
        ImageGraph.Parameters params =
        		new ImageGraph.Parameters(320, 240, false);
        
        mImageGraph = new ImageGraph(params);
        
        startNetwork(remote, mImageGraph);
        startHttp();
        startLocalPreview();
                
        ((Button)findViewById(R.id.pref_button)).
        	setOnClickListener(preferencesListener);
        ((Button)findViewById(R.id.focus_button)).
        	setOnClickListener(focusListener);        
        ((ToggleButton)findViewById(R.id.switch_streaming)).
        	setOnCheckedChangeListener(streamingListener);        
        ((ToggleButton)findViewById(R.id.switch_camera)).
        	setOnCheckedChangeListener(frontCameraListener);
    }
    
    OnClickListener preferencesListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			startActivity(new Intent(this_activity, CameraPreferences.class));
		}
	};
    
    OnClickListener focusListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mImageGraph.focus();
		}
	};
	
	OnCheckedChangeListener streamingListener = new OnCheckedChangeListener()
	{
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked)
		{
			if (isChecked) {
				mImageGraph.start();
			}
			else {
				mImageGraph.stop();
			}
		}
	};
    
    protected void setUseFrontCamera(boolean enabled) {
		ImageGraph.Parameters oldParams = mImageGraph.getParameters();
		ImageGraph.Parameters params =
				new ImageGraph.Parameters(
						oldParams.width,
						oldParams.height,
						enabled);
		mImageGraph.setParameters(params);
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