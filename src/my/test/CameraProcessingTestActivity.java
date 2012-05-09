package my.test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import my.test.net.http.HttpServer;
import my.test.net.http.HttpServer.Handler;
import my.test.net.http.HttpServer.ResponseCode;
import my.test.net.tcp.TcpUnicastClient;
import my.test.net.tcp.TcpUnicastServer;
import my.test.ui.StartServerActivity;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.VideoView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

public class CameraProcessingTestActivity extends Activity {
    
	CameraServer preview;
	
	public void onPreferencesClick() {
		//startActivity(new Intent(this, CameraPreferences.class));
		startActivityForResult(
				new Intent(this, StartServerActivity.class),
				0);
	}
	
	protected void startNetwork(final VideoView remoteView,
			String remoteAddr, CameraServer cameraServer)
	{
		ImageSink sink = null;
		TcpUnicastClient src = null;
		
		int port = 45678;
		try {
			sink = new TcpUnicastServer(port, cameraServer.getExecutor());
		}
		catch (Exception e) {
			Log.e("xcam", "failed to create sink");
		}
		
		try {
			src = new TcpUnicastClient();
			src.connect(remoteAddr, port);
		}
		catch (Exception e) {
			Log.e("xcam", "failed to create source", e);
		}
		
		src.setOnFrameBitmapCallback(new ImageSource.OnFrameBitmapCallback() {
			@Override
			public void onFrame(Bitmap bitmap) {
				if (bitmap == null) {
					return;
				}
				
				synchronized(CameraServer.class) {
					SurfaceHolder surfaceHolder = remoteView.getHolder();
					Canvas canvas = surfaceHolder.lockCanvas();		
					canvas.drawBitmap(bitmap, 0, 0, null);
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		});
		
		cameraServer.addImageSink(sink);
	}
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        VideoView local = (VideoView)findViewById(R.id.view_local);
        SharedPreferences prefs =
        		PreferenceManager.getDefaultSharedPreferences(this);
 
        final String keyRemoteAddr = getString(R.string.key_pref_remote_addr);
        final String remoteAddr = prefs.getString(keyRemoteAddr, "127.0.0.1");
        final VideoView remote = (VideoView)findViewById(R.id.view_remote);
        
        preview = new CameraServer(local);
        
        new Thread() {
        	@Override
        	public void run() {
            	startNetwork(remote, remoteAddr, preview);        		
        	};
        }.start();
        
        try {
        	HttpServer srv = new HttpServer(8080);
        	srv.addHandler("test", new Handler() {
				
				@Override
				public boolean handle(Map<String, String> params,
						OutputStream outputStream) throws IOException {
					PrintWriter pw = new PrintWriter(outputStream);
					pw.write("Content-Type:text/http\n");
					pw.write("\n");
					
					pw.write("<head>");
					pw.write("<title>Test Http response</title>");
					pw.write("</head>");
					
					pw.write("<body><table>\n");
					for (Map.Entry<String, String> param : params.entrySet()) {
						pw.write("<tr>");
						
						pw.write("<th>");
						pw.write(param.getKey());
						pw.write("</th>");
						
						pw.write("<th>");
						pw.write(param.getValue());
						pw.write("</th>");
						
						pw.write("</tr>");
					}
					pw.write("</table></body>");
					pw.flush();
					
					return true;
				}
			});
        }
        catch (Exception e) {
        	Log.e("camera processing", "failed to start HTTP server");
        }
        
        ((Button)findViewById(R.id.pref_button)).setOnClickListener(
	        new OnClickListener() {
				@Override
				public void onClick(View v) {
					onPreferencesClick();
				}
			});
        
        ((Button)findViewById(R.id.focus_button)).setOnClickListener(
        	new OnClickListener() {
				@Override
				public void onClick(View v) {
					preview.focus();
				}
        	});
        
        ToggleButton s = (ToggleButton)findViewById(R.id.switch_streaming);
        s.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					preview.start();
				}
				else {
					preview.stop();
				}
			}
		});
        
        s = (ToggleButton)findViewById(R.id.switch_camera);
        s.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				preview.setUseFrontCamera(isChecked);
			}
		});
    }
}