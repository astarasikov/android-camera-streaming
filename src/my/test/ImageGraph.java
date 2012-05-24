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

import my.test.image.ImageProcessor;

class ImageGraph {
	public static class Parameters {
		public final int width;
		public final int height;
		public final boolean frontCamera;
		
		public Parameters(int width, int height, boolean front)
		{
			this.width = width;
			this.height = height;
			this.frontCamera = front;
		}
	}
	
	protected Parameters mParameters = null;
	protected ImageProcessor mImageProcessor = null;
	
	protected boolean mCameraOpened = false;
	Camera camera = null;
	
	List<ImageSink> imageSinks = new LinkedList<ImageSink>();
	
	BlockingQueue<Runnable> sinkRunners =
			new LinkedBlockingQueue<Runnable>();
	ThreadPoolExecutor sinkExecutor =
			new ThreadPoolExecutor(2, 4, 100,
					TimeUnit.MILLISECONDS, sinkRunners);
	
	public ImageGraph(Parameters parameters, ImageProcessor imageProcessor)
	{
		mImageProcessor = imageProcessor;
		setParameters(parameters);
	}
	
	public ThreadPoolExecutor getExecutor() {
		return sinkExecutor;
	}
	
	public synchronized void setParameters(Parameters parameters) {
		this.mParameters = parameters;
		restart();
	}
	
	public synchronized void setImageProcessor(ImageProcessor imageProcessor) {
		this.mImageProcessor = imageProcessor;
	}
	
	public synchronized Parameters getParameters() {
		return mParameters;
	}
	
	public synchronized void addImageSink(ImageSink imageSink) {
		imageSinks.add(imageSink);
	}
	
	public synchronized void removeImageSink(ImageSink imageSink) {
		imageSinks.remove(imageSink);
	}
	
	protected synchronized void restart() {
		stopCamera();
		startCamera();
	}
	
	public synchronized void teardown() {
		stopCamera();
		sinkExecutor.purge();
		for (ImageSink imageSink : imageSinks) {
			imageSink.close();
		}
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
			boolean isFrontCam = ci.facing == CameraInfo.CAMERA_FACING_FRONT;
			if (isFrontCam == mParameters.frontCamera) {
				goodCameraIndex = i;
				break;
			}
		}
		if (goodCameraIndex < 0) {
			goodCameraIndex = 0;
		}
		return Camera.open(goodCameraIndex);
	}
	
	protected void sendCameraImage(Bitmap bitmap) {
		synchronized(imageSinks) {
			for (ImageSink sink : imageSinks) {
				try {
					sink.send(bitmap);
				} catch (Exception e) {
					e.printStackTrace();
				}	
			}
		}
	}

	protected synchronized void startCamera() {
		if (mCameraOpened) {
			return;
		}
		
		CameraInfo cameraInfo = new CameraInfo();
		if ((camera = openCamera(cameraInfo)) == null) {
			return;
		}
		mCameraOpened = true;

		final Parameters params = this.mParameters;
		
		Camera.Parameters cameraParameters = camera.getParameters();
		cameraParameters.setPreviewSize(params.width, params.height);
		camera.setParameters(cameraParameters);
		
		final int cameraAngle = cameraInfo.orientation;
		
		CameraYUVPreviewCallback cb = new CameraYUVPreviewCallback(camera);
		cb.setOnFrameCallback(new ImageSource.OnFrameRawCallback() {			
			@Override
			public void onFrame(int[] rgbBuffer, int width, int height) {
				Bitmap bmp = Bitmap.createBitmap(rgbBuffer, width, height,
						Bitmap.Config.RGB_565);
				if (mImageProcessor != null) {
					Bitmap filtered = mImageProcessor
							.filter(rgbBuffer, width, height);
					bmp = mImageProcessor.process(bmp, filtered, cameraAngle);
				}
				sendCameraImage(bmp);
			}
		});
		camera.setPreviewCallback(cb);
		camera.startPreview();
	}

	protected synchronized void stopCamera() {
		if (!mCameraOpened) {
			return;
		}
		camera.stopPreview();
		camera.setPreviewCallback(null);
		camera.release();
		camera = null;
		mCameraOpened = false;
	}
}
