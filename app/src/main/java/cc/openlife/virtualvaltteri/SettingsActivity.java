package cc.openlife.virtualvaltteri;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.fragment.app.Fragment;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        // below line is to change
        // the title of our action bar.
        //getSupportActionBar().setTitle("Settings");

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
                    DynamicMultiSelectListPreference driversPreference = (DynamicMultiSelectListPreference) preference;
                    Set<String> driversValuesCurrent = driversPreference.getReducedValues(true);
                    System.out.println("pref change handler, getReducedValues: " + driversValuesCurrent);
                    ArrayList<CharSequence> csList = new ArrayList<>(Collections.emptyList());
                    HashSet<CharSequence> valueAsHash = (HashSet<CharSequence>)value;
                    for(CharSequence cs: valueAsHash){
                        System.out.println(cs);
                        csList.add(cs);
                    }
                    returnIntent.putCharSequenceArrayListExtra("settings_drivers",csList);
                    setResult(Activity.RESULT_OK,returnIntent);
                    return true;
                }
                // Logic: All the drivers ever followed remain in driversPreference. We just don't show them in the list or getSummary();
                // Only once unselected from this list do we also remove the same from driversPreference.
                // In mathematical terms, a driver is removed if they aren't driving in this session and aren't selected in favoritedDrivers;
                if(preference.getKey().equals("favorited_drivers_key")){
                    System.out.println("favorited_drivers_key");
                    System.out.println(value);
                    final MultiSelectListPreference favoritedDriversPreference = (MultiSelectListPreference) preference;
                    Set<String> newOldFollows = (HashSet<String>) value;
                    Set<String> oldOldFollows = favoritedDriversPreference.getValues();
                    System.out.println("olldoldfollows"+ oldOldFollows);
                    System.out.println("newoldfollows"+newOldFollows);
                    // Subtract
                    // Note that for this widget you can only ever unselect widgets
                    assert newOldFollows.size() <= oldOldFollows.size();
                    oldOldFollows.removeAll(newOldFollows);
                    Set<String> diff =  oldOldFollows;
                    System.out.println("diff"+ diff);

                    SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
                    Set<String> currentFollows = new HashSet<>();
                    currentFollows = p.getStringSet("drivers_key", currentFollows);
                    System.out.println("currentfollows"+ currentFollows);

                    currentFollows.removeAll(diff);
                    System.out.println("newCurrentFollows "+ currentFollows);
                    p.edit().putStringSet("drivers_key", currentFollows);

                    favoritedDriversPreference.setValues(newOldFollows);

                    ArrayList<String> a = new ArrayList<>();
                    a.addAll(currentFollows);
                    returnIntent.putStringArrayListExtra("settings_drivers",a);
                    setResult(Activity.RESULT_OK,returnIntent);
                    return true;
                }
                return false;
            }
        };

        SettingsFragment mySettingsFragment = new SettingsFragment(prefChanged, sortedDriversArray);
        // below line is used to check if
        // frame layout is empty or not.
        if (findViewById(R.id.settingsLayout) != null) {
            // below line is to inflate our fragment.
            getSupportFragmentManager().beginTransaction().add(R.id.settingsLayout, mySettingsFragment).commit();
        }
    }
}
