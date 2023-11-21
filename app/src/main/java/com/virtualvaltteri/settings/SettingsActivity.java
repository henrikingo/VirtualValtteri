package com.virtualvaltteri.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;

import com.virtualvaltteri.MainActivity;
import com.virtualvaltteri.R;
import com.virtualvaltteri.sensors.CollectFg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class SettingsActivity extends AppCompatActivity {

    public SortedSet<String> sortedDrivers2;
    public Intent returnIntent;
    static SettingsFragment mySettingsFragment;
    Preference.OnPreferenceChangeListener prefChanged;
    public SettingsActivity(){
        super();
    }

    public SettingsActivity(@LayoutRes int res){
        super(res);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(savedInstanceState==null) {
            //This happens on first create and is apparently ok...
        } else if (savedInstanceState.size()==0){
            System.out.println("Yup it was null...");
            //Silly Android...
            savedInstanceState.putInt("foo",0);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Follow drivers
        returnIntent = new Intent();

        prefChanged = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object value) {
                System.out.println("prefChanged: " + preference.getKey() + " = " + value);
                if(preference.getKey().equals("drivers_key")){

                    ArrayList<CharSequence> csList = new ArrayList<>(Collections.emptyList());
                    SortedSet<CharSequence> sortedValue = new ConcurrentSkipListSet<>((HashSet<CharSequence>)value);
                    for(CharSequence cs: sortedValue){
                        System.out.println(cs);
                        csList.add(cs);
                    }
                    returnIntent.putCharSequenceArrayListExtra("settings_drivers",csList);
                    setResult(Activity.RESULT_OK,returnIntent);
                    return true;
                }
                if(preference.getKey().equals("collect_sensor_data_key")){
                    CollectFg collect = new CollectFg(getApplicationContext());
                    // This ensures that if you flip on sensors, the foreground notification will appear in 10 seconds.
                    collect.startServiceStandby();
                    return true;
                }
                return true;
            }
        };

        SharedPreferences p = getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        this.sortedDrivers2 = new ConcurrentSkipListSet<>();
        this.sortedDrivers2.addAll(p.getStringSet("sorted_drivers_key", new HashSet<>()));
        if(mySettingsFragment==null) {
            mySettingsFragment = new SettingsFragment(prefChanged, sortedDrivers2);
        }
        FrameLayout v = findViewById(R.id.settingsLayout);
        System.out.println(v.getChildCount());
        if (v != null) {
            // below line is to inflate our fragment.
            getSupportFragmentManager().beginTransaction().add(R.id.settingsLayout, mySettingsFragment).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("Settings onResume" + sortedDrivers2);
        mySettingsFragment.sortedDrivers2= sortedDrivers2.toArray(new CharSequence[0]);
        mySettingsFragment.refreshEverything();
        System.out.println("Settings onResume" + sortedDrivers2);
    }

    public void onStop (){
        super.onStop();
        System.out.println("Settings onStop");
        System.out.println(sortedDrivers2);
        System.out.println(mySettingsFragment.driversPreference.getReducedValues(true));
        Set<String> values = mySettingsFragment.driversPreference.getReducedValues(true);
        returnIntent.putCharSequenceArrayListExtra("follow_driver_names_key", new ArrayList<>(values));
        System.out.println(returnIntent.toString());
        System.out.println(returnIntent.getExtras().toString());
        setResult(Activity.RESULT_OK,returnIntent);

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        prefs.edit().putStringSet("follow_driver_names_key", values).commit();

    }
}
