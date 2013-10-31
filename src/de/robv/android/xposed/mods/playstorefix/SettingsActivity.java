package de.robv.android.xposed.mods.playstorefix;

import android.support.v7.app.ActionBarActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import me.piebridge.android.preference.PreferenceFragment;
import android.widget.Toast;

public class SettingsActivity extends ActionBarActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.abc_screen);
		findViewById(R.id.title_container).setVisibility(android.view.View.GONE);

        // Display the fragment as the main content.
        if (savedInstanceState == null)
			getSupportFragmentManager().beginTransaction().replace(R.id.action_bar_activity_content,
	                new PrefsFragment()).commit();
	}

	public static class PrefsFragment extends PreferenceFragment
			implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// this is important because although the handler classes that read these settings
			// are in the same package, they are executed in the context of the hooked package
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			addPreferencesFromResource(R.xml.preferences);
			
			SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();
			sharedPref.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(sharedPref, "density");
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals("density")) {
				EditTextPreference pref = (EditTextPreference) findPreference(key);
				String value = sharedPreferences.getString(key, "");
				if (value.isEmpty()) {
					value = "(unchanged)";
				} else if (!value.matches("\\d{2,3}")) {
					pref.setText("");
					Toast.makeText(getActivity(), "Invalid density value", Toast.LENGTH_SHORT).show();
					return;
				}
				
				if (isAdded())
					pref.setSummary(getString(R.string.pref_density_summary, value));
			}
		}
	}
}
