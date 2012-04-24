/*
 * Class.java
 * 
 * Copyright (c) 2012 Alexander Tarasikov <alexander.tarasikov@gmail.com>. 
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
 * along with this program.  If not, see <http ://www.gnu.org/licenses/>.
 */
package my.test;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.VideoView;

import java.util.Arrays;

import my.test.net.TcpUnicastClient;
import my.test.net.TcpUnicastServer;

class CameraPreview implements SurfaceHolder.Callback {
	// Stores hardware camera state

	protected boolean mCameraOpened = false;
	// Stores the request from external logic (i.e, UI)
	protected boolean mNeedsPreview = false;
	
	//whether front or rear camera is used
	protected boolean mUseFrontCamera = false;
	Camera camera = null;
	VideoView localView;
	VideoView remoteView;
	String remoteAddr;
	
	public CameraPreview(VideoView localView, VideoView remoteView,
			String remoteAddr)
	{
		this.localView = localView;
		this.remoteView = remoteView;
		this.remoteAddr = remoteAddr;
		localView.getHolder().addCallback(this);
		remoteView.getHolder().addCallback(this);
	}

	public synchronized void start() {
		mNeedsPreview = true;
		restartCamera();
	}

	public synchronized void stop() {
		mNeedsPreview = false;
		restartCamera();
	}
	
	public synchronized void setUseFrontCamera(boolean enabled) {
		mUseFrontCamera = enabled;
		restartCamera();
	}
	
	protected Camera openCamera() {
		int numberOfCameras = Camera.getNumberOfCameras();
		if (numberOfCameras == 0) {
			return null;
		}
		
		int goodCameraIndex = -1;
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo ci = new CameraInfo();
			Camera.getCameraInfo(i, ci);
			if ((ci.facing == CameraInfo.CAMERA_FACING_FRONT) == mUseFrontCamera) {
				goodCameraIndex = i;
				break;
			}
		}
		if (goodCameraIndex < 0) {
			goodCameraIndex = 0;
		}
		return Camera.open(goodCameraIndex);
	}

	ImageSink sink = null;
	TcpUnicastClient src = null;
	
	protected void startNetwork() {
		int port = 45678;
		try {
			sink = new TcpUnicastServer(port);
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
				
				synchronized(CameraPreview.class) {
					SurfaceHolder surfaceHolder = remoteView.getHolder();
					Canvas canvas = surfaceHolder.lockCanvas();		
					canvas.drawBitmap(bitmap, 0, 0, null);
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		});		
	}
	
	protected void cameraBitmap(Bitmap bitmap) {
		if (sink != null) {
			try {
				sink.send(bitmap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
	}

	public synchronized void startCamera() {
		if (mCameraOpened) {
			return;
		}
		
		if ((camera = openCamera()) == null) {
			return;
		}
		
		new Thread() {
			@Override
			public void run() {
				startNetwork();
			}
		}.start();
		
		Camera.Parameters params = camera.getParameters();
		params.setPreviewSize(320, 240);
		camera.setParameters(params);

		CameraYUVPreviewCallback cb = new CameraYUVPreviewCallback(camera);
		cb.setOnFrameCallback(new ImageSource.OnFrameRawCallback() {
			@Override
			public void onFrame(int[] rgbBuffer, int width, int height) {
				ImageProcessing.preProcess(rgbBuffer, width, height);
				Bitmap bmp = Bitmap.createBitmap(rgbBuffer, width, height,
						Bitmap.Config.RGB_565);
				cameraBitmap(bmp);
				
				SurfaceHolder surfaceHolder = localView.getHolder();
				Canvas canvas = surfaceHolder.lockCanvas();		
				canvas.drawBitmap(bmp, 0, 0, null);
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		});
		camera.setPreviewCallback(cb);
		camera.startPreview();
		
		mCameraOpened = true;
	}

	public synchronized void stopCamera() {
		if (!mCameraOpened) {
			return;
		}
		camera.stopPreview();
		camera.release();
		camera = null;
		mCameraOpened = false;
	}

	public synchronized void restartCamera() {
		stopCamera();
		if (mNeedsPreview) {
			startCamera();
		}
	}

	public void surfaceCreated(SurfaceHolder sh) {
		restartCamera();
	}

	public void surfaceChanged(SurfaceHolder sh, int i, int i1, int i2) {
		restartCamera();
	}

	public void surfaceDestroyed(SurfaceHolder sh) {
		stopCamera();
	}
}
