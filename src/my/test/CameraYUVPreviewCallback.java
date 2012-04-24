package my.test;

import android.hardware.Camera;

public class CameraYUVPreviewCallback implements Camera.PreviewCallback,
	ImageSource
{
	OnFrameRawCallback onFrameCallback;
	int rgbBuffer[];
	byte yuvBuffer[];
	
	synchronized void realloc(Camera cam) {
		Camera.Size size = cam.getParameters().getPreviewSize();
		int bufSize = size.width * size.height * 3;
		if (rgbBuffer != null && rgbBuffer.length == bufSize) {
			return;
		}
		rgbBuffer = new int[bufSize];
		yuvBuffer = new byte[bufSize * 2];
	}
	
	public CameraYUVPreviewCallback(Camera cam) {
		realloc(cam);
		cam.addCallbackBuffer(yuvBuffer);
	}
	
	@Override
	public synchronized void onPreviewFrame(byte[] data, Camera camera) {
		realloc(camera);
		
		Camera.Size params = camera.getParameters().getPreviewSize();
		int width = params.width;
		int height = params.height;
		
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
}
