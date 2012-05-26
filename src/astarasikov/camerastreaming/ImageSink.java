package astarasikov.camerastreaming;

import android.graphics.Bitmap;

public interface ImageSink {
	public void send(Bitmap bitmap) throws Exception;
	public void close();
}
