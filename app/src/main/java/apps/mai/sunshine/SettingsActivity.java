package apps.mai.sunshine;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {
    Toolbar toolbar;
    private static final String SETTINGS_FRAG_TAG = "settings";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction().replace(R.id.content_settings,new settingsFragment(),
                SETTINGS_FRAG_TAG).commit();

    }

    public static class settingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener{
        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);

            // add preferences that defined in xml folder
            addPreferencesFromResource(R.xml.pref_general);
            //for all preferences, we attach OnPreferenceChangeListener to update summery UI
            // when the preference changes
            bindPreferenceSummeryToValue(findPreference(getString(R.string.pref_location_key)));
            bindPreferenceSummeryToValue(findPreference(getString(R.string.pref_units_key)));

        }
        private void bindPreferenceSummeryToValue(Preference preference){
            preference.setOnPreferenceChangeListener(this);
            onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString(preference.getKey(),""));
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            String stringValue=o.toString();
            if (preference instanceof ListPreference){
                ListPreference listPreference= (ListPreference) preference;
                int prefIndex=listPreference.findIndexOfValue(stringValue);
                if (prefIndex>=0){
                    preference.setSummary(listPreference.getEntries()[prefIndex]);
                }
            }
            else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    }




}
