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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
	final static String LOG_TAG =
			CameraProcessingTestActivity.class.getSimpleName();
	
	static {
		Log.i(LOG_TAG, "loading dsp-jni");
		System.loadLibrary("dsp-jni");
	}
	
	static Bitmap fitBitmap(Bitmap bitmap, int width, int height) {
		if (bitmap == null) {
			return bitmap;
		}
		
		int bmpW = bitmap.getWidth();
		int bmpH = bitmap.getHeight();
		
		int dstWidth = bmpW;
		int dstHeight = bmpH;
		
		if (bmpW < height && bmpH < height) {
			return bitmap;
		}
		
		if (dstWidth > width) {
			dstWidth = width;
			dstHeight = (int)(bmpH * ((1.0 * width) / bmpW));
		}
		if (dstHeight > height) {
			dstHeight = height;
			dstWidth = (int)(bmpW * ((1.0 * height) / bmpH));
		}
		
		return Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, false);		
	}
	
	static public class VideoViewSink
		implements ImageSource.OnFrameBitmapCallback,
		ImageSink
	{
		VideoView videoView;
		long lastFrameTime;
		boolean drawFps;
		Paint mPaint;
		PreferenceHelper mPreferenceHelper;
		
		void initFpsCounter() {
			drawFps = mPreferenceHelper.booleanPreference(
					R.string.key_pref_draw_fps, false);
			
			if (!drawFps) {
				return;
			}
			
			mPaint = new Paint();
			mPaint.setColor(Color.GREEN);
			mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setTextSize(20);
			mPaint.setTypeface(Typeface.DEFAULT_BOLD);
		}
		
		public VideoViewSink(VideoView videoView,
				PreferenceHelper preferenceHelper)
		{
			this.videoView = videoView;
			this.mPreferenceHelper = preferenceHelper;
			initFpsCounter();
		}
		
		void drawBitmap(Bitmap bitmap) {	
			if (bitmap == null) {
				return;
			}
			
			int w = videoView.getWidth();
			int h = videoView.getHeight();
			
			bitmap = fitBitmap(bitmap, w, h);
			if (bitmap == null) {
				return;
			}
			
			int left = (w - bitmap.getWidth()) / 2;
			int top = (h - bitmap.getHeight()) / 2;
			
			SurfaceHolder surfaceHolder = videoView.getHolder();
			Canvas canvas = surfaceHolder.lockCanvas();
			
			canvas.drawBitmap(bitmap, left, top, null);
			
			if (drawFps) {
				long timeNow = System.currentTimeMillis();
				if (lastFrameTime != 0) {
					float fps = 1000.0f / (timeNow - lastFrameTime);
					String text = String.format("FPS %.2f", fps);
					canvas.drawText(text, left + 30, top + 30, mPaint);
				}
				lastFrameTime = timeNow;
			}
			
			surfaceHolder.unlockCanvasAndPost(canvas);	
		}
	
		@Override
		public void onFrame(Bitmap bitmap) {
			drawBitmap(bitmap);
		}

		@Override
		public void send(Bitmap bitmap) throws Exception {
			drawBitmap(bitmap);			
		}

		@Override
		public void close() {
			/* nothing to do here */
		}
	}
		
	CameraProcessingTestActivity this_activity = this;
	ImageGraph mImageGraph;
	PreferenceHelper mPreferenceHelper;
	SharedPreferences mSharedPreferences;
	
	int mWidth = 320;
	int mHeight = 240;
	
	final static int defaultNetworkPort = 8082;
	final static int defaultHttpPort = 8080;
	
	protected void startNetworkServer() throws Exception {
        boolean startServer =
        		mPreferenceHelper
        			.booleanPreference(R.string.key_pref_server, true);
        if (!startServer) {
        	return;
        }
        int localPort =
        		mPreferenceHelper
        			.intPreference(R.string.key_pref_local_port,
        					defaultNetworkPort);
        
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
        			.intPreference(R.string.key_pref_remote_port,
        					defaultNetworkPort);
		
		tcpClient = new TcpUnicastClient();
		tcpClient.connect(remoteAddr, remotePort);
							
		VideoViewSink callback =
				new VideoViewSink(videoView, mPreferenceHelper);
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
        				.intPreference(R.string.key_pref_http_local_port,
        						defaultHttpPort);
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
        ImageSink videoViewSink = new VideoViewSink(local, mPreferenceHelper);
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
	
	protected void getPreferedResolution() {
		String resolutionString =
				mPreferenceHelper
					.stringPreference(R.string.key_pref_resolution, "320x240");
		String WnH[] = resolutionString.split("x");
		mWidth = Integer.valueOf(WnH[0]);
		mHeight = Integer.valueOf(WnH[1]);
	}
	
	protected synchronized void restartServer() {
        ImageUtils.setUseNative(
        		mPreferenceHelper.booleanPreference(R.string.key_pref_nativeyuv,
        				false));
        
        stopServer();
        boolean shouldStream =
        		((ToggleButton)findViewById(R.id.switch_streaming)).isChecked();
        if (!shouldStream) {
        	return;
        }
        
        boolean useFrontCamera = 
            	((ToggleButton)findViewById(R.id.switch_camera)).isChecked();
        
        getPreferedResolution();
        
        ImageGraph.Parameters params =
        		new ImageGraph.Parameters(mWidth, mHeight, useFrontCamera);
        ImageProcessor imageProcessor
        	= new ImageProcessor(this, mPreferenceHelper);
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
        
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferenceHelper = new PreferenceHelper(this, mSharedPreferences);
        
        mSharedPreferences.registerOnSharedPreferenceChangeListener(
        		preferenceChangeListener);
                                
        ((Button)findViewById(R.id.pref_button)).
        	setOnClickListener(preferencesListener);
        ((ToggleButton)findViewById(R.id.switch_streaming)).
        	setOnCheckedChangeListener(streamingListener);        
        ((ToggleButton)findViewById(R.id.switch_camera)).
        	setOnCheckedChangeListener(frontCameraListener);
        ((VideoView)findViewById(R.id.view_local)).
        	setOnClickListener(focusListener);
        ((VideoView)findViewById(R.id.view_remote)).
        	setOnClickListener(focusListener);
    }
    
    OnSharedPreferenceChangeListener preferenceChangeListener = 
    		new OnSharedPreferenceChangeListener()
    {	
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key)
		{
			restartServer();
		}
	};
    
    OnClickListener preferencesListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			stopServer();				
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
			restartServer();
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