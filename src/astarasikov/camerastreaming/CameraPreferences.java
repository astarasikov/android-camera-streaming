/*
 * This file is part of CameraStreaming application.
 * CameraStreaming is an application for Android for streaming
 * video over MJPEG and applying DSP effects like convolution
 *
 * Copyright (C) 2012 Alexander Tarasikov <alexander.tarasikov@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package astarasikov.camerastreaming;

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
