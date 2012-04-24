package my.test.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.net.Socket;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import my.test.ImageSink;

public class TcpUnicastServer implements ImageSink {
	final static String LOG_TAG = TcpUnicastServer.class.getSimpleName();

	List<Socket> clients = Collections
			.synchronizedList(new LinkedList<Socket>());

	public TcpUnicastServer(int port) throws Exception {
		final ServerSocket ss = new ServerSocket(port);
		ss.setReuseAddress(true);
		new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					try {
						clients.add(ss.accept());
					} catch (IOException e) {
						Log.e(LOG_TAG, "failed to accept socket", e);
					}
				}
			}
		}.start();
	}

	@Override
	public void send(Bitmap bitmap) throws Exception {
		final Bitmap bmp = bitmap;
		new Thread() {
			@Override
			public void run() {
				synchronized (clients) {
					for (Socket s : clients) {
						try {
							bmp.compress(CompressFormat.JPEG, 60,
									s.getOutputStream());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}.start();
	}
}
