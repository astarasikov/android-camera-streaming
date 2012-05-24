package my.test;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.text.InputType;

public class CameraPreferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.main_preferences);
	
		makeNumericInput(R.string.key_pref_http_local_port);
		makeNumericInput(R.string.key_pref_remote_port);
		makeNumericInput(R.string.key_pref_local_port);
		makeNumericInput(R.string.key_ar_max_faces);
	}
	
	protected void makeNumericInput(int keyId) {
		String string = getString(keyId);
		EditTextPreference preference =
				(EditTextPreference)findPreference(string);
		preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
	}
}
