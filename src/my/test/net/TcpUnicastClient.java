package my.test.net;

import java.net.Socket;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import my.test.ImageSource;

public class TcpUnicastClient implements ImageSource {
	final static String LOG_TAG = TcpUnicastClient.class.getSimpleName();
	
	OnFrameRawCallback rawCallback;
	OnFrameBitmapCallback bitmapCallback;
	Socket socket;
	Thread thread;
	
	protected synchronized void readFrame() {
		if (socket == null || socket.isClosed()) {
			return;
		}
		try {
			Bitmap bitmap = BitmapFactory.decodeStream(socket.getInputStream());
			if (bitmapCallback != null) {
				bitmapCallback.onFrame(bitmap);
			}
			
			if (rawCallback != null) {
				int w = bitmap.getWidth();
				int h = bitmap.getHeight();
				int rgb[] = new int[w * h];
				bitmap.getPixels(rgb, 0, w, 0, 0, w, h);
				rawCallback.onFrame(rgb, w, h);
			}
		}
		catch (Exception e) {
			Log.d(LOG_TAG, "failed to decode bitmap", e);
		}
	}
	
	@Override
	public synchronized void setOnFrameCallback(OnFrameRawCallback callback) {
		this.rawCallback = callback;
	}
	
	@Override
	public void setOnFrameBitmapCallback(OnFrameBitmapCallback callback) {
		this.bitmapCallback = callback;
	}
	
	public synchronized void connect(String dstName, int port) throws Exception {
		close();
		socket = new Socket(dstName, port);
		thread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					readFrame();
				}
			}
		};
		thread.start();
	}
	
	public synchronized void close() {
		if (socket == null || socket.isClosed()) {
			return;
		}
		
		if (thread != null) {
			try {
				thread.interrupt();
				thread.join();
				thread = null;
			}
			catch (Exception e) {
				Log.d(LOG_TAG, "failed to wait for thread to die", e);
			}
		}
		
		try {
			socket.close();
		}
		catch (Exception e) {
			Log.e(LOG_TAG, "failed to close socket", e);
		}
		socket = null;
	}
}
