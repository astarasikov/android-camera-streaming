package astarasikov.camerastreaming;

import astarasikov.camerastreaming.image.ImageUtils;
import android.hardware.Camera;

public class CameraYUVPreviewCallback implements Camera.PreviewCallback,
	ImageSource
{
	OnFrameRawCallback onFrameCallback;
	int width;
	int height;
	
	int rgbBuffer[];
	byte yuvBuffer[];
	
	synchronized void allocateBuffers() {
		int bufSize = width * height;
		int yuvSize = (bufSize * 3) / 2;
		rgbBuffer = new int[bufSize];		
		yuvBuffer = new byte[yuvSize];
	}
	
	public CameraYUVPreviewCallback(Camera camera) {
		Camera.Size params = camera.getParameters().getPreviewSize();
		width = params.width;
		height = params.height;
		allocateBuffers();
		camera.addCallbackBuffer(yuvBuffer);
	}
	
	@Override
	public synchronized void onPreviewFrame(byte[] data, Camera camera) 
	{
		if (onFrameCallback != null) {
			ImageUtils.decodeYUV420SP(rgbBuffer, data, width, height);
			onFrameCallback.onFrame(rgbBuffer, width, height);
		}
		camera.addCallbackBuffer(yuvBuffer);
	}

	@Override
	public synchronized void setOnFrameCallback(OnFrameRawCallback callback) {
		this.onFrameCallback = callback;
	}

	@Override
	public void setOnFrameBitmapCallback(OnFrameBitmapCallback callback) {
		
	}

	@Override
	public void close() {
		rgbBuffer = null;
		yuvBuffer = null;
	}
}
