package com.virtualvaltteri;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.virtualvaltteri.sensors.CollectFg;
import com.virtualvaltteri.settings.SettingsActivity;

import org.w3c.dom.Text;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public final static String SHARED_PREFS_MAGIC_WORD = "com.virtualvaltteri_preferences";
    public static MainActivity mainActivityInstance;
    private TextView mTextView;
    private TextView[] mTextViewLarge;
    private TextView[] mTextViewCarNr;
    private TextView[] mTextViewPosition;
    private TextView[] mTextViewLarge2;
    private TextView[] mTextViewCarNr2;
    private TextView[] mTextViewPosition2;
    private CollectFg collect;
    private String englishMessage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Flip back the shutdown key. Without this app will close in seconds.
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        prefs.edit().putString("system_close_key", "On").commit();


        System.out.println("Creating main app activity...");
        mainActivityInstance=this;
        // Initialize UI elements
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.text_view);
        mTextViewLarge = new TextView[]{
                findViewById(R.id.idLargeText),
                findViewById(R.id.driver2_idLargeText),
                findViewById(R.id.driver3_idLargeText)
        };
        mTextViewCarNr = new TextView[]{
                findViewById(R.id.idCarNr),
                findViewById(R.id.driver2_idCarNr),
                findViewById(R.id.driver3_idCarNr)
        };
        mTextViewPosition = new TextView[]{
                findViewById(R.id.idPosition),
                findViewById(R.id.driver2_idPosition),
                findViewById(R.id.driver3_idPosition)
        };
        mTextViewLarge2 = new TextView[]{
                findViewById(R.id.idLargeText2),
                findViewById(R.id.driver2_idLargeText2),
                findViewById(R.id.driver3_idLargeText2)
        };
        mTextViewCarNr2 = new TextView[]{
                findViewById(R.id.idCarNr2),
                findViewById(R.id.driver2_idCarNr2),
                findViewById(R.id.driver3_idCarNr2)
        };
        mTextViewPosition2 = new TextView[]{
                findViewById(R.id.idPosition2),
                findViewById(R.id.driver2_idPosition2),
                findViewById(R.id.driver3_idPosition2)
        };


        Button settingsBtn = findViewById(R.id.settingsButton);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
                prefs.edit().putString("seen_hint", "true").commit();
                // opening a new intent to open settings activity.
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });


        setHintVisibility();

        // This is the main audio stream managed by your hardware up-down key
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Run also when in background
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

        if (!pm.isIgnoringBatteryOptimizations(packageName)){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            });
        }


        final int REQUEST_WRITE_PERMISSION = 786;
        // Check if we have write permission
        int permission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // prompt the user
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
        }

    }

    protected String cutDecimal(String d){
        int dot = d.indexOf(".");
        if (dot >= 1){
            return d.substring(0,dot+2);
        }
        return d;
    }
    private void setHintVisibility(){
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        TextView mHint = (TextView)findViewById(R.id.idTextHint);
        Set<String> followDriverNames = prefs.getStringSet("follow_driver_names_key", new HashSet<>());

        if(prefs.contains("seen_hint") && mHint!=null) {
            mHint.setVisibility(View.INVISIBLE);
        } else if ( followDriverNames.isEmpty() && (prefs.getString("writein_driver_name_key", "")).equals("")) {
            mHint.setVisibility(View.VISIBLE);
        }
        else {
            mHint.setVisibility(View.INVISIBLE);
            prefs.edit().putString("seen_hint", "true").commit();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("MainActivity.onActivityResult() " +requestCode + " " + resultCode + " " + data);

        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        if(prefs.getString("system_close_key", "On").equals("Close")){
            //System.out.println("Closing VirtualValtteri. Reason: User selected Close in Settings.");
            //closeApp();
        }

        System.out.println("Just signal VirtualValtteriService to applyPreferences()");
        collect.applyPreferences();
        setHintVisibility();
    }
    public void closeApp(){
        if(vvs!=null) vvs.unsubscribe(mainLooper.mHandler);
        //unbindService(vvsCallbacks);
        collect.close();
        // Flip back the shutdown key so we are able to start the next time
        //prefs.edit().putString("system_close_key", "On").commit();
        System.out.println("Siri how do I kill an android process");
        finishAndRemoveTask();
    }
    private void setColorAll(int color){
        setColorAll(color, 0);
    }
        private void setColorAll(int color, int driver){
        if(mTextViewPosition==null || mTextViewPosition[0]==null)
            return;


        mTextViewPosition[driver].setTextColor(color);
        mTextViewCarNr[driver].setTextColor(color);
        mTextViewLarge[driver].setTextColor(color);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.runFinalizersOnExit(true);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private VirtualValtteriService vvs;
    MainLooperThread mainLooper;


    class MainLooperThread extends Thread {
        public Handler mHandler;
        public void run() {
            Looper.prepare();

            mHandler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    Map<String,String> englishMessageMap = (Map<String, String>) msg.obj;
                    processMesssage(englishMessageMap);
                    collect.ping();
                }
            };
            vvs.subscribe(mainLooper.mHandler);
            while(vvs.getPastMessages().size()>0){
                Map<String,String> e = vvs.getPastMessages().remove(0);
                processMesssage(e);
            }

            Looper.loop();
        }
    }

    private void queueLoop(){
        System.out.println("queueLoop()");
        // Make sure looper is setup before we unleash the sensor event listeners
        mainLooper = new MainLooperThread();
        mainLooper.start();
    }
    /** Defines callbacks for service binding, passed to bindService(). */
    private ServiceConnection vvsCallbacks = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            VirtualValtteriService.VirtualValtteriBinder binder = (VirtualValtteriService.VirtualValtteriBinder) service;
            vvs = binder.getService();
            assert vvs!=null;

            //binder.linkToDeath(() -> {System.out.println("binderDied");closeApp();},0);

            System.out.println("VVS Bound");
            queueLoop();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            System.out.println("VVS Disconnected");
        }
    };

    protected void onResume (){
        super.onResume();
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Configure the behavior of the hidden system bars.
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        if(collect==null){
            System.out.println("Create Collect object to manage sensors");
            collect = new CollectFg(this);
        }
        collect.startServiceStandby();

        refreshEverything();
        Intent intent = new Intent(this, VirtualValtteriService.class);
        bindService(intent, vvsCallbacks, Context.BIND_AUTO_CREATE);


    }
    private void refreshEverything()
    {
        onActivityResult(0,0,null);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, VirtualValtteriService.class);
        bindService(intent, vvsCallbacks, Context.BIND_AUTO_CREATE);
    }

    protected void onPause() {
        super.onPause();
        if(vvs!=null) vvs.unsubscribe(mainLooper.mHandler);
        unbindService(vvsCallbacks);
    }

    private void rollOldValuesBack(){rollOldValuesBack(0);}
    private void rollOldValuesBack(int driver)
    {
        if(mTextViewLarge[driver]!=null && mTextViewLarge2[driver]!=null)
            mTextViewLarge2[driver].setText(mTextViewLarge[driver].getText());
        if(mTextViewCarNr[driver]!=null && mTextViewCarNr2[driver]!=null)
            mTextViewCarNr2[driver].setText(mTextViewCarNr[driver].getText());
        if(mTextViewPosition[driver]!=null && mTextViewPosition2[driver]!=null)
            mTextViewPosition2[driver].setText(mTextViewPosition[driver].getText());
    }
    public void processMesssage(Map<String,String> englishMessageMap) {
        String englishMessage = englishMessageMap.get("message");
        //if(!englishMessage.equals("")) System.out.println("processMessage " + englishMessageMap);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    int driverSlot = 0;
                    if(englishMessageMap.containsKey("driverSlot") && mTextViewLarge[1]!=null){
                        driverSlot = Integer.parseInt( englishMessageMap.get("driverSlot") );
                    }

                    if (englishMessageMap.containsKey("s1")||
                            englishMessageMap.containsKey("s2")||
                            englishMessageMap.containsKey("lap")||
                            (englishMessageMap.containsKey("position")))
                        rollOldValuesBack();

                    textViewAppend(englishMessage);
                    if(englishMessageMap.containsKey("s1") && mTextViewLarge!=null)
                        mTextViewLarge[driverSlot].setText(cutDecimal(englishMessageMap.get("s1")));
                    if(englishMessageMap.containsKey("s2") && mTextViewLarge!=null)
                        mTextViewLarge[driverSlot].setText(cutDecimal(englishMessageMap.get("s2")));
                    if(englishMessageMap.containsKey("lap") && mTextViewLarge!=null)
                        mTextViewLarge[driverSlot].setText(cutDecimal(englishMessageMap.get("lap")));
                    if(englishMessageMap.containsKey("carNr") && mTextViewCarNr!=null) {
                            if (false && !(englishMessageMap.get("carNr").equals(mTextViewCarNr[driverSlot].getText())))
                                // Reset the position field since we don't know the position of the new car
                                mTextViewPosition[driverSlot].setText("");
                            mTextViewCarNr[driverSlot].setText(englishMessageMap.get("carNr"));
                    }
                    // Of course if we have the position, great :-)
                    if(englishMessageMap.containsKey("position") && !englishMessageMap.get("position").equals("") && mTextViewPosition!=null)
                        mTextViewPosition[driverSlot].setText("P"+englishMessageMap.get("position"));
                    if(englishMessageMap.containsKey("time_meta")){
                        if(englishMessageMap.get("time_meta").equals("improved"))
                            setColorAll(getColor(R.color.timeImproved));
                        else if(englishMessageMap.get("time_meta").equals("individual best"))
                            setColorAll(getColor(R.color.timeIndividualBest));
                        else if(englishMessageMap.get("time_meta").equals("best"))
                            setColorAll(getColor(R.color.timeBest));
                        else
                            setColorAll(getColor(R.color.timeNormal));
                    }
               }
            });
        }
        public void textViewAppend(String str){
            if(mTextView!=null)
                mTextView.setText(mTextView.getText() + " " + str);
        }
    }