package net.georgewhiteside.android.abstractart;

import sheetrock.panda.changelog.ChangeLog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	private SharedPreferences sharedPreferences;
	private ChangeLog changelog;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	    addPreferencesFromResource(R.xml.settings);
	    
	    setAutoCycleTimeState();
	    
	    changelog = new ChangeLog(this);
	    
	    // set up changelog preference 
	    
	    final Preference changelogPref = (Preference) getPreferenceManager().findPreference("changelog");
	    changelogPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {            
            public boolean onPreferenceClick(Preference preference) {
                changelog.getFullLogDialog().show();
                return true;
            }
	    });
	    
	    // set up donate link
	    
	    final Preference donatePref = (Preference) getPreferenceManager().findPreference("donate");
	    donatePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {            
            public boolean onPreferenceClick(Preference preference) {
                // try to launch google play to view donate app; if it fails (not installed, etc.) launch the browser version
                final String appName = "net.georgewhiteside.android.abstractartarabicaataraxis";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
                } catch (android.content.ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
                }
                return true;
            }
        });
	    
	    // set up contact author preference
	    
	    final Preference contactPref = (Preference) getPreferenceManager().findPreference("contact");
	    contactPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {            
            public boolean onPreferenceClick(Preference preference) {
                Intent send = new Intent(Intent.ACTION_SENDTO);
                String uriText;

                uriText = "mailto:george@georgewhiteside.net" + 
                          "?subject=Abstract Art";
                uriText = uriText.replace(" ", "%20");
                Uri uri = Uri.parse(uriText);

                send.setData(uri);
                startActivity(Intent.createChooser(send, "Send e-mail"));
                return true;
                
            }
        });
	    
	    if(changelog.firstRun()) {
	        changelog.getLogDialog().show();
	    }
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
	
	private void setAutoCycleTimeState() {
		String autoCycleBehavior = sharedPreferences.getString("autoCycleBehavior", null);
		
		// update dependent preference enabled state
		
		Preference dependentPref = (Preference) getPreferenceManager().findPreference("autoCycleTime");
		
		if(autoCycleBehavior.equals("interval")) {
			dependentPref.setEnabled(true);
		} else {
			dependentPref.setEnabled(false);
		}
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// automatically set listpreference summaries to reflect their current values
		// update: I decided these were kind of silly and presented an inconsistent user interface experience
	    // Preference preference = findPreference(key);
	    // setSummary(preference);
		
		if(key.equals("autoCycleBehavior")); {
			setAutoCycleTimeState();
		}
		
	    // check if palette cycling was just checked; if the bug is present, give the user a warning:
	    if(key.equals("enablePaletteEffects")) {
	    	boolean infoPaletteBugDetected = sharedPreferences.getBoolean("infoPaletteBugDetected", false); // only initialized (to true) in Wallpaper.java
	    	boolean enablePaletteEffects = sharedPreferences.getBoolean(key, false);
	    	if(infoPaletteBugDetected && enablePaletteEffects) {
	    		Toast.makeText(this, "Warning: palette effects don't seem to work on your phone (yet).", Toast.LENGTH_LONG).show();
	    	}
	    }
	    
	    Wallpaper.refresh(); // bit of a hack to ensure output settings appear ASAP
	}
}
