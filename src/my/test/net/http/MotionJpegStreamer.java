package my.test.net.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;
import android.widget.FrameLayout;

import my.test.ImageSink;

public class MotionJpegStreamer implements HttpServer.Handler,
	ImageSink
{
	final static String LOG_TAG = MotionJpegStreamer.class.getSimpleName();
	Bitmap lastFrame = null;
	final long frameDate[] = new long[] { 0 };
	
	class MotionJpegThread extends Thread {
		final OutputStream outputStream;
		final PrintStream printStream;
		final Object synchronizer;
				
		public MotionJpegThread(OutputStream outputStream, Object synchronizer)
				throws IOException
		{
			this.outputStream = outputStream;
			this.synchronizer = synchronizer;
			printStream = new PrintStream(outputStream);
		}
		
		@Override
		public void run() {
			Bitmap lastBitmap = null;
			Bitmap currentBitmap = null;
			
			try {
				headerMJPG(printStream);
				while (!isInterrupted()) {
					synchronized (synchronizer) {
						currentBitmap = lastFrame;
					}
					
					if (currentBitmap == lastBitmap) {
						continue;
					}
					
					lastBitmap = currentBitmap;
					if (currentBitmap == null) {
						continue;
					}
					
					long dateDiff = System.currentTimeMillis() - frameDate[0];
					if (dateDiff > 50) {
						continue;
					}
										
					headerJPG(printStream);
					printStream.flush();
					currentBitmap.compress(CompressFormat.JPEG,
							60, outputStream);
				}				
			}
			catch (IOException e) {
				Log.e(LOG_TAG, "failed to stream JPEG", e);
			}
		};
	}

	@Override
	public boolean handle(Map<String, String> params, OutputStream outputStream)
			throws IOException
	{
		Thread streamThread = new MotionJpegThread(outputStream, this);
		streamThread.start();
		try {
			streamThread.join();
		} catch (InterruptedException e) {
			Log.e(LOG_TAG, "failed to wait for thread completion", e);
			return false;
		}
		return true;
	}
	
	final static byte EOL[] = {
		(byte)'\r', (byte)'\n'
	};
	
	final static String uuidMJPG = "7b3cc56e5f51db803f790dad720ed50a";
	final static String boundaryMJPG = "--" + uuidMJPG;
	final static String mimeMJPG =
				"multipart/x-mixed-replace;boundary=" + uuidMJPG;
	final static String cacheControl =
			"Cache-Control: no-store"
			+ ", no-cache, must-revalidate"
			+ ", pre-check=0, post-check=0, max-age=0";
	
	protected void headerMJPG(PrintStream ps) throws IOException {
		ps.print("Connection: Close");
		ps.write(EOL);
		
		ps.print("Server: Test");
		ps.write(EOL);
		
		ps.print(cacheControl);
		ps.write(EOL);
		
		ps.print("Pragma: no-cache");
		ps.write(EOL);
		
		String dateString = new Date().toString();
		
		ps.print("Date: " + dateString);
		ps.write(EOL);
		
		ps.print("Last Modified: " + dateString);
		ps.write(EOL);
		
		ps.print("Content-type: " + mimeMJPG);
		ps.write(EOL);
	}
	
	protected void headerJPG(PrintStream ps) throws IOException {
		ps.write(EOL);
		ps.print(boundaryMJPG);
		ps.write(EOL);
		ps.print("Content-type: image/jpeg");
		ps.write(EOL);
		ps.write(EOL);
	}

	@Override
	public synchronized void send(Bitmap bitmap) throws Exception {
		lastFrame = bitmap;
		frameDate[0] = System.currentTimeMillis();
	}

	@Override
	public void teardown() {
		
	}
}
