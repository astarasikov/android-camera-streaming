package my.test;

import my.test.image.ImageProcessor;
import my.test.image.ImageUtils;
import my.test.net.http.HttpServer;
import my.test.net.http.MotionJpegStreamer;
import my.test.net.tcp.TcpUnicastClient;
import my.test.net.tcp.TcpUnicastServer;
import my.test.utils.PreferenceHelper;
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
	static {
		Log.i("NATIVE", "loading dsp-jni");
		System.loadLibrary("dsp-jni");
	}
	
	static void drawBitmap(Bitmap bitmap, VideoView videoView) {
		if (bitmap == null || videoView == null) {
			return;
		}
		
		SurfaceHolder surfaceHolder = videoView.getHolder();
		Canvas canvas = surfaceHolder.lockCanvas();		
		canvas.drawBitmap(bitmap, 0, 0, null);
		surfaceHolder.unlockCanvasAndPost(canvas);	
	}
	
	static class VideoViewSink implements ImageSink {
		VideoView videoView;
		
		public VideoViewSink(VideoView videoView) {
			this.videoView = videoView;
		}
		
		@Override
		public void send(Bitmap bitmap) throws Exception {
			drawBitmap(bitmap, videoView);
		}

		@Override
		public void close() {
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
			drawBitmap(bitmap, videoView);
		}
	}
	
	final static String LOG_TAG =
			CameraProcessingTestActivity.class.getSimpleName();
	
	CameraProcessingTestActivity this_activity = this;
	ImageGraph mImageGraph;
	PreferenceHelper mPreferenceHelper;

	
	protected void startNetworkServer() throws Exception {
        boolean startServer =
        		mPreferenceHelper
        			.booleanPreference(R.string.key_pref_server, true);
        if (!startServer) {
        	return;
        }
        int localPort =
        		mPreferenceHelper
        			.intPreference(R.string.key_pref_local_port, 8082);
        
		ImageSink tcpServer = null;
		tcpServer = new TcpUnicastServer(localPort,
				mImageGraph.getExecutor());
		mImageGraph.addImageSink(tcpServer);
	}
	
	protected void startNetworkClient(VideoView videoView) throws Exception {
        boolean startClient =
        		mPreferenceHelper
        			.booleanPreference(R.string.key_pref_client, true);
        if (!startClient) {
        	return;
        }
		TcpUnicastClient tcpClient = null;
        String remoteAddr =
        		mPreferenceHelper.stringPreference(R.string.key_pref_remote_addr,
        				"127.0.0.1");
		int remotePort =
        		mPreferenceHelper
        			.intPreference(R.string.key_pref_remote_port, 8082);
		
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
        	boolean startHttp =
        			mPreferenceHelper
        				.booleanPreference(R.string.key_pref_http_server, true);
        	if (!startHttp) {
        		return;
        	}
        	
        	int httpPort =
        			mPreferenceHelper
        				.intPreference(R.string.key_pref_http_local_port, 8080);
            MotionJpegStreamer mjpgStreamer = new MotionJpegStreamer();
            mImageGraph.addImageSink(mjpgStreamer);
        	HttpServer srv = new HttpServer(httpPort);
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
	
	protected synchronized void stopServer() {
        if (mImageGraph != null) {
        	mImageGraph.teardown();
        }		
	}
	
	protected synchronized void startServer() {
        ImageUtils.setUseNative(
        		mPreferenceHelper.booleanPreference(R.string.key_pref_nativeyuv,
        				false));
        
        ImageGraph.Parameters params =
        		new ImageGraph.Parameters(320, 240, false);
        
        ImageProcessor imageProcessor = new ImageProcessor(this);
        
        stopServer();
        mImageGraph = new ImageGraph(params, imageProcessor);
       
        VideoView remote = (VideoView)findViewById(R.id.view_remote);
        startNetwork(remote, mImageGraph);
        startHttp();
        startLocalPreview();
	}
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mPreferenceHelper = new PreferenceHelper(this);
                        
        ((Button)findViewById(R.id.pref_button)).
        	setOnClickListener(preferencesListener);
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
			if (mImageGraph == null) {
				return;
			}
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
				startServer();
			}
			else {
				stopServer();
			}
		}
	};
    
    protected void setUseFrontCamera(boolean enabled) {
    	if (mImageGraph == null) {
    		return;
    	}
    	
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