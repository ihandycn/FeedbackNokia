package com.nokia.feedbacktonokia;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class NPSIntroActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "NPSIntroActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_page);

        // add button listener
        ((Button) findViewById(R.id.intro_page_ok)).setOnClickListener(this);
        ((Button) findViewById(R.id.intro_page_later)).setOnClickListener(this);

        // hide the action bar icon
        // getActionBar().setIcon(R.drawable.fake_action_icon);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            getActionBar().setDisplayShowHomeEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        if (NPSUtils.DEBUG) {
            Log.i(TAG,
                    "NPSIntroActivity onClick(), disable receiver and stop schedule alarms");
        }
        // disable the BOOT_COMPLETED receiver, and stop to schedule the alarms
        int flag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        ComponentName component = new ComponentName(this,
                NPSBootBroadcastReceiver.class);
        getPackageManager().setComponentEnabledSetting(component, flag,
                PackageManager.DONT_KILL_APP);
        
        // disable data change receiver 
        ComponentName component2 = new ComponentName(this,
                NPSDateChangedReceiver.class);
        getPackageManager().setComponentEnabledSetting(
                component2, flag, PackageManager.DONT_KILL_APP);

        // clean the alarm
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(this, NPSAlarmReceiver.class), 0);
        am.cancel(pendingIntent);

        if (v.getId() == R.id.intro_page_ok) {
            // start NPSActivity
            Intent i = new Intent(this, NPSActivity.class);
            i.setData(Uri.parse("nps://launch_feedbacktonokia/notification"));
            startActivity(i);

            // close self
            finish();
        } else if (v.getId() == R.id.intro_page_later) {
            finish();
        }
    }
}
