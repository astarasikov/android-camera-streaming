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
import android.view.SurfaceHolder;
import android.widget.VideoView;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import my.test.image.ImageProcessing;

class CameraServer implements SurfaceHolder.Callback {
	// Stores hardware camera state

	protected boolean mCameraOpened = false;
	// Stores the request from external logic (i.e, UI)
	protected boolean mNeedsPreview = false;
	
	//whether front or rear camera is used
	protected boolean mUseFrontCamera = false;
	Camera camera = null;
	VideoView localView;
	
	List<ImageSink> imageSinks = new LinkedList<ImageSink>();
	
	BlockingQueue<Runnable> sinkRunners =
			new LinkedBlockingQueue<Runnable>();
	ThreadPoolExecutor sinkExecutor =
			new ThreadPoolExecutor(2, 4, 100,
					TimeUnit.MILLISECONDS, sinkRunners);
	
	public CameraServer(VideoView localView)
	{
		this.localView = localView;
		localView.getHolder().addCallback(this);
	}
	
	//FIXME: decide on which class should initiate server connections
	public ThreadPoolExecutor getExecutor() {
		return sinkExecutor;
	}
		
	public synchronized void addImageSink(ImageSink imageSink) {
		imageSinks.add(imageSink);
	}
	
	public synchronized void removeImageSink(ImageSink imageSink) {
		imageSinks.remove(imageSink);
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
	
	public synchronized void focus() {
		if (!mCameraOpened) {
			return;
		}
		camera.autoFocus(null);
	}
	
	protected Camera openCamera(CameraInfo ci) {
		int numberOfCameras = Camera.getNumberOfCameras();
		if (numberOfCameras == 0) {
			return null;
		}
		
		int goodCameraIndex = -1;
		for (int i = 0; i < numberOfCameras; i++) {
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
	
	protected void cameraBitmap(Bitmap bitmap) {
		for (ImageSink sink : imageSinks) {
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
		
		CameraInfo cameraInfo = new CameraInfo();
		if ((camera = openCamera(cameraInfo)) == null) {
			return;
		}
		
		Camera.Parameters params = camera.getParameters();
		//params.setPreviewSize(640, 480);
		params.setPreviewSize(320, 240);
		camera.setParameters(params);
		
		final int cameraAngle = cameraInfo.orientation;
		
		CameraYUVPreviewCallback cb = new CameraYUVPreviewCallback(camera);
		cb.setOnFrameCallback(new ImageSource.OnFrameRawCallback() {
			int tmpBuffer[] = new int[640 * 480];
			
			@Override
			public void onFrame(int[] rgbBuffer, int width, int height) {
				ImageProcessing.preProcess(rgbBuffer, tmpBuffer, width, height);				
				Bitmap bmp = Bitmap.createBitmap(rgbBuffer, width, height,
						Bitmap.Config.RGB_565);
								
				bmp = ImageProcessing.process(bmp, cameraAngle);
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
		camera.setPreviewCallback(null);
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
