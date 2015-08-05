package com.nokia.feedbacktonokia;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class NPSBootBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "NPSBootBroadcastReceiver";

    private PendingIntent mAlarmSender;
    private boolean forTest = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (NPSUtils.DEBUG) {
            Log.e(TAG, "NPSBootBroadcastReceiver onReceive(), context:"+context);
        }

        // We want the alarm to go off 15 minutes from now.
        long firstTime = SystemClock.elapsedRealtime();

        // Create an IntentSender that will launch our service, to be scheduled
        // with the alarm manager.
        Intent i = new Intent(context, NPSAlarmReceiver.class);
        mAlarmSender = PendingIntent.getBroadcast(context, 0, i, 0);

        // Schedule the alarm!
        AlarmManager am = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        // 30 * 1000 & 60*1000 only for test, we need
        // AlarmManager.INTERVAL_FIFTEEN_MINUTES
        // and AlarmManager.INTERVAL_HOUR
        if (forTest) {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    firstTime + 60 * 1000, 3 * 60 * 1000, mAlarmSender);
        } else {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, firstTime
                    + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
					AlarmManager.INTERVAL_HALF_HOUR, mAlarmSender);
        }
    }

}
