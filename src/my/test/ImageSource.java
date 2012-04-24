package my.test;

import android.graphics.Bitmap;

public interface ImageSource {
	public void setOnFrameCallback(OnFrameRawCallback callback);
	public void setOnFrameBitmapCallback(OnFrameBitmapCallback callback);
	
	public static interface OnFrameRawCallback {
		public void onFrame(int rgb[], int width, int height);
	}
	
	public static interface OnFrameBitmapCallback {
		public void onFrame(Bitmap bitmap);
	}
}
