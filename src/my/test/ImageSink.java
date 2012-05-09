package my.test;

import android.graphics.Bitmap;

public interface ImageSink {
	public void send(Bitmap bitmap) throws Exception;
	public void teardown();
}
