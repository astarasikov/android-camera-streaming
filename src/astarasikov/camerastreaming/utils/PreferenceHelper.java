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
package astarasikov.camerastreaming.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceHelper {
	Context mContext;
	SharedPreferences mSharedPreferences;
	
	public String stringPreference(int keyId, String defaultValue) {
		String key = mContext.getString(keyId);
		return mSharedPreferences.getString(key, defaultValue);
	}
	
	public int intPreference(int keyId, Integer defaultValue) {
		String key = mContext.getString(keyId);
		String stringValue = mSharedPreferences.getString(key,
				defaultValue.toString());
		return Integer.valueOf(stringValue);
	}
	
	public boolean booleanPreference(int keyId, Boolean defaultValue) {
		String key = mContext.getString(keyId);
		return mSharedPreferences.getBoolean(key, defaultValue);
	}
	
	public PreferenceHelper(Context context,
			SharedPreferences sharedPreferences)
	{
		this.mContext = context;
		mSharedPreferences = sharedPreferences;
	}
}
