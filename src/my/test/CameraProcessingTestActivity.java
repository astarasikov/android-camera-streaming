package my.test;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.VideoView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

public class CameraProcessingTestActivity extends Activity {
    
	CameraPreview preview;
	
	public void onPreferencesClick() {
		startActivity(new Intent(this, CameraPreferences.class));
	}
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        VideoView local = (VideoView)findViewById(R.id.view_local);
        VideoView remote = (VideoView)findViewById(R.id.view_remote);
        
        SharedPreferences prefs =
        		PreferenceManager.getDefaultSharedPreferences(this);
        String keyRemoteAddr = getString(R.string.key_pref_remote_addr);
        String remoteAddr = prefs.getString(keyRemoteAddr, "127.0.0.1");
        
        preview = new CameraPreview(local, remote, remoteAddr);
        
        ((Button)findViewById(R.id.pref_button)).setOnClickListener(
	        new OnClickListener() {
				@Override
				public void onClick(View v) {
					onPreferencesClick();
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