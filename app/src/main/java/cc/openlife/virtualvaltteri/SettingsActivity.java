package cc.openlife.virtualvaltteri;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class SettingsActivity extends AppCompatActivity {

    private ArrayList<CharSequence> sortedDriversFromIntent;
    private CharSequence[] sortedDriversArray;
    private Intent returnIntent;
    public SettingsActivity(){
        super();
    }

    public SettingsActivity(@LayoutRes int res){
        super(res);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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
                    SortedSet<CharSequence> sortedValue = (ConcurrentSkipListSet<CharSequence>)value;
                    for(CharSequence cs: sortedValue){
                        System.out.println(cs);
                        csList.add(cs);
                    }
                    returnIntent.putCharSequenceArrayListExtra("settings_drivers",csList);
                    setResult(Activity.RESULT_OK,returnIntent);
                    return true;
                }
                return true;
            }
        };

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
        SettingsFragment mySettingsFragment = new SettingsFragment(prefChanged, sortedDriversArray, returnIntent);
        // below line is used to check if
        // frame layout is empty or not.
        if (findViewById(R.id.settingsLayout) != null) {
            // below line is to inflate our fragment.
            getSupportFragmentManager().beginTransaction().add(R.id.settingsLayout, mySettingsFragment).commit();
        }
    }
}
