package net.georgewhiteside.android.abstractart;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	    addPreferencesFromResource(R.xml.settings);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		// automatically set listpreference summaries to reflect their current values
	    Preference preference = findPreference(key);

	    if (preference instanceof ListPreference) {
	        ListPreference listPreference = (ListPreference)preference;
	        preference.setSummary(listPreference.getEntry());
	        Log.i("bbdebug", (String) listPreference.getEntry());
	    }
	}
}





