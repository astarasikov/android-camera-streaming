package my.test.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.ListView;

public class StartServerActivity extends Activity {
	protected void createDialog() {
		AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle("Choose your fate, mortal")
			.setSingleChoiceItems(new String[]{
					"Custom Protocol",
					"Http MJPEG",
					"RTP H264",
			}, -1,
					new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					})
			.setPositiveButton("foo", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
				}
				
			})
			.setNegativeButton("cancel", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
				
			})
			.create();
		dialog.show();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		createDialog();
	}
}
