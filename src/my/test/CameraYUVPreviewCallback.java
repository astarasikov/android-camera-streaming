package my.test;

import my.test.image.ImageUtils;
import android.hardware.Camera;
import android.util.Log;

public class CameraYUVPreviewCallback implements Camera.PreviewCallback,
	ImageSource
{
	OnFrameRawCallback onFrameCallback;
	int rgbBuffer[];
	byte yuvBuffer[];
	
	synchronized void realloc(Camera cam) {
		Camera.Size size = cam.getParameters().getPreviewSize();
		int bufSize = size.width * size.height;
		int yuvSize = (bufSize * 3) / 2;
		
		boolean rgbIsGood = rgbBuffer != null && rgbBuffer.length == bufSize;
		boolean yuvIsGood = yuvBuffer != null && yuvBuffer.length == yuvSize;
		
		if (!rgbIsGood) {
			rgbBuffer = new int[bufSize];		
		}
		
		if (!yuvIsGood) {
			yuvBuffer = new byte[yuvSize];
		}
	}
	
	public CameraYUVPreviewCallback(Camera camera) {
		realloc(camera);
		camera.addCallbackBuffer(yuvBuffer);
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
