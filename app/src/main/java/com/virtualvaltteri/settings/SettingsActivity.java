package com.virtualvaltteri.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.virtualvaltteri.MainActivity;
import com.virtualvaltteri.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class SettingsActivity extends AppCompatActivity {

    private ArrayList<CharSequence> sortedDriversFromIntent;
    public CharSequence[] sortedDriversArray;
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
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sortedDriversFromIntent = extras.getCharSequenceArrayList("sortedDrivers");
            sortedDriversArray = sortedDriversFromIntent.toArray(new CharSequence[sortedDriversFromIntent.size()]);
        }
        returnIntent = new Intent();

        prefChanged = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object value) {
                System.out.println("prefChanged: " + preference.getKey() + " = " + value);
                if(preference.getKey().equals("speaker_key")){
                    returnIntent.putExtra("settings_speaker",value.toString());
                    setResult(Activity.RESULT_OK,returnIntent);
                    return true;
                }
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
                return true;
            }
        };


        SharedPreferences p = getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        if(mySettingsFragment==null) {
            mySettingsFragment = new SettingsFragment(prefChanged, sortedDriversArray);
        }
        FrameLayout v = findViewById(R.id.settingsLayout);
        System.out.println(v.getChildCount());
        if (v != null) {
            // below line is to inflate our fragment.
            getSupportFragmentManager().beginTransaction().add(R.id.settingsLayout, mySettingsFragment).commit();
        }
    }
    public void onStop (){
        super.onStop();
        setResult(Activity.RESULT_OK,returnIntent);

    }
}
