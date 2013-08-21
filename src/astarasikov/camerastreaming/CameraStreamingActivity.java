/*
 * This file is part of CameraStreaming application.
 * CameraStreaming is an application for Android for streaming
 * video over MJPEG and applying DSP effects like convolution
 *
 * Copyright (C) 2012 Alexander Tarasikov <alexander.tarasikov@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package astarasikov.camerastreaming;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;
import android.widget.VideoView;
import astarasikov.camerastreaming.image.ImageProcessor;
import astarasikov.camerastreaming.image.ImageUtils;
import astarasikov.camerastreaming.net.http.HttpServer;
import astarasikov.camerastreaming.net.http.MotionJpegStreamer;
import astarasikov.camerastreaming.net.tcp.TcpUnicastClient;
import astarasikov.camerastreaming.net.tcp.TcpUnicastServer;
import astarasikov.camerastreaming.utils.PreferenceHelper;

public class CameraStreamingActivity extends Activity {
	final static String LOG_TAG =
			CameraStreamingActivity.class.getSimpleName();
	
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
		
	CameraStreamingActivity this_activity = this;
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
			@Override
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
	
	private final static String uidFroyoSdk = "9774D56D682E549C";
	protected boolean isRunningOnEmulator() {
		String androidId = Settings.Secure.ANDROID_ID;
		return "sdk".equals(Build.PRODUCT) 
				|| "google_sdk".equals(Build.PRODUCT)
				|| androidId == null
				|| androidId == uidFroyoSdk;
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
        
        int screenAngle = 0;
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        
        switch (display.getRotation()) {
        case Surface.ROTATION_270:
        	screenAngle = 270;
        	break;
        case Surface.ROTATION_180:
        	screenAngle = 180;
        	break;
        case Surface.ROTATION_90:
        	screenAngle = 90;
        	break;
        }
        
        /*
         * Camera orientation is somehow effed up on emulator
         */
        if (isRunningOnEmulator()) {
        	screenAngle -= 90;
        }
                                
        ImageGraph.Parameters params =
        		new ImageGraph.Parameters(mWidth,
        				mHeight, useFrontCamera, screenAngle);
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
    
    OnClickListener preferencesListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			stopServer();
			((ToggleButton)findViewById(R.id.switch_streaming))
				.setChecked(false);
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
						enabled,
						oldParams.screenAngle);
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
