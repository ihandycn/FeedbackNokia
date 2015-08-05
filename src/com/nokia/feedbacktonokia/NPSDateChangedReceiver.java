package com.nokia.feedbacktonokia;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class NPSDateChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "NPSDateChangedReceiver";

    // for testing, set it to true. for product, set it to false.
    private boolean isTesting = false;

    @Override
    public void onReceive(Context context, Intent intent) {

		Log.i(TAG, "NPSDateChangedReceiver onReceive: " + intent);

        if (isTesting) {
            // For test: if ModifySystemTime has been started, then do nothing
            ActivityManager am = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);
            List<RunningTaskInfo> list = am.getRunningTasks(100);
            for (RunningTaskInfo info : list) {
                if (info.topActivity.getPackageName().equals(
                        "com.example.modifysystemtime")
                        && info.baseActivity.getPackageName().equals(
                                "com.example.modifysystemtime")) {
                    // find it, return
                    Log.i(TAG,
                            "NPSAlarmReceiver onReceive() modifysystemtime has started");
                    System.exit(0);
                    return;
                }
            }
        }

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        int daysElapsed = preferences.getInt(NPSUtils.DAYS_ELAPSED, 0);
        long currentRTC = System.currentTimeMillis();
		long rtc0 = preferences.getLong(NPSUtils.FIRST_BOOTUP_TIME, 0);
		long last_record_time = preferences.getLong(NPSUtils.LAST_RECORD_TIME,
				0);
		long new_rtc0 = rtc0;

		if (rtc0 == 0 || last_record_time == 0) {
			Log.e(TAG, "NPSDateChangedReceiver get preferences error!!");
			System.exit(0);
			return;
		}
        if (NPSUtils.DEBUG) {
            Log.i(TAG, "NPSDateChangedReceiver currentRTC:" + currentRTC);
            Log.i(TAG,
                    "NPSDateChangedReceiver current time:"
                            + NPSUtils.getTimeFromTimeMillis(currentRTC));
            Log.i(TAG,
                    "NPSDateChangedReceiver origin FTU time:"
							+ NPSUtils.getTimeFromTimeMillis(rtc0));
			Log.i(TAG,
					"NPSDateChangedReceiver last record time:"
							+ NPSUtils.getTimeFromTimeMillis(last_record_time));
            Log.i(TAG, "NPSDateChangedReceiver daysElapsed:" + daysElapsed);
        }

		long rtc_gap = currentRTC - last_record_time;
		if (rtc_gap < 0) {
			rtc_gap = -rtc_gap;
		}

		if (rtc_gap < NPSUtils.MILLIS_A_DAY) {
			Log.i(TAG, "NPSDateChangedReceiver: fine tune the time.");
			System.exit(0);
			return;
		}
		new_rtc0 = currentRTC - daysElapsed * NPSUtils.MILLIS_A_DAY;

		if (NPSUtils.DEBUG) {
			Log.i(TAG, "NPSDateChangedReceiver rtc0:" + new_rtc0);
			Log.i(TAG,
					"NPSDateChangedReceiver FTU time:"
							+ NPSUtils.getTimeFromTimeMillis(new_rtc0));
		}

		preferences.edit().putLong(NPSUtils.FIRST_BOOTUP_TIME, new_rtc0)
				.commit();
		System.exit(0);

    }

}
