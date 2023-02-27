package cc.openlife.virtualvaltteri;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.preference.Preference;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class SettingsActivity extends AppCompatActivity {

    private ArrayList<CharSequence> sortedDriversFromIntent;
    private CharSequence[] sortedDriversArray;
    private Intent returnIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // below line is to change
        // the title of our action bar.
        getSupportActionBar().setTitle("Settings");

        // Follow drivers
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sortedDriversFromIntent = extras.getCharSequenceArrayList("sortedDrivers");
            sortedDriversArray = sortedDriversFromIntent.toArray(new CharSequence[sortedDriversFromIntent.size()]);
        }
        returnIntent = new Intent();


        Preference.OnPreferenceChangeListener prefChanged = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object value) {
                System.out.println("prefChanged: " + preference.getKey() + " = " + value);
                if(preference.getKey().equals("speaker_key")){
                    returnIntent.putExtra("settings_speaker",value.toString());
                    setResult(Activity.RESULT_OK,returnIntent);
                    return true;
                }
                if(preference.getKey().equals("drivers_key")){
                    ArrayList<CharSequence> csList = new ArrayList<>(Collections.emptyList());
                    HashSet<CharSequence> valueAsHash = (HashSet<CharSequence>)value;
                    for(CharSequence cs: valueAsHash){
                        csList.add(cs);
                    }
                    returnIntent.putCharSequenceArrayListExtra("settings_drivers",csList);
                    setResult(Activity.RESULT_OK,returnIntent);
                    return true;
                }
                return false;
            }
        };


        // below line is used to check if
        // frame layout is empty or not.
        if (findViewById(R.id.settingsLayout) != null) {
/*            if (savedInstanceState != null) {
                return;
            }
*/
            // below line is to inflate our fragment.
            getSupportFragmentManager().beginTransaction().add(R.id.settingsLayout, new SettingsFragment(prefChanged, sortedDriversArray)).commit();
        }
    }
}
