package my.test.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
	
	public PreferenceHelper(Context context) {
		this.mContext = context;
		mSharedPreferences =
				PreferenceManager.getDefaultSharedPreferences(context);        
	}
}
