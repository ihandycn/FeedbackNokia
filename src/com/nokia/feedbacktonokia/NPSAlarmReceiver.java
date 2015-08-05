package com.nokia.feedbacktonokia;

import java.util.Calendar;
import java.util.List;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class NPSAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "NPSAlarmReceiver";

    private SharedPreferences preferences;
    // Use a layout id for a unique identifier
    public static int NPS_NOTIFICATIONS = R.id.nps_layout;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (NPSUtils.DEBUG) {
            Log.i(TAG, "NPSAlarmReceiver onReceive(), context:" + context);
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // first time bootup, add share preference data.
        if (preferences.getBoolean(NPSUtils.FIRST_BOOTUP_FLAG, true) == true) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(NPSUtils.FIRST_BOOTUP_FLAG, false)
                    .putLong(NPSUtils.FIRST_BOOTUP_TIME,
                            System.currentTimeMillis())
					.putLong(NPSUtils.LAST_RECORD_TIME,
							System.currentTimeMillis())
                    .putInt(NPSUtils.DAYS_ELAPSED, 0).commit();
            ;
            System.exit(0);
            return;
        }

        // Increment the total days elapsed
        incrementElapsedDays();

        // check whether to remind user
        if (isTimeOutInDays() == false || isNotificationAllow(context) == false
                || isValidTimeslot() == false) {
            System.exit(0);
            return;
        }

        // if NPSActivity has already been started, then do nothing
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> list = am.getRunningTasks(100);
        for (RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(
                    "com.nokia.feedbacktonokia")
                    && info.baseActivity.getPackageName().equals(
                            "com.nokia.feedbacktonokia")) {
                // find it, return
                return;
            }
        }

        NotificationManager NManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, NPSIntroActivity.class),
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification n = new Notification.Builder(context)
                .setContentTitle(context.getText(R.string.app_name))
                .setContentText(context.getText(R.string.rate_your_phone))
                .setSmallIcon(R.drawable.def_icon)
                .setWhen(System.currentTimeMillis()).setDefaults(0)
//                .addKind(Notification.KIND_EVENT)
                .setContentIntent(contentIntent).build();

        n.flags |= Notification.FLAG_AUTO_CANCEL;

        NManager.notify(NPS_NOTIFICATIONS, n);
        System.exit(0);
    }

    private void incrementElapsedDays() {
        long rtc0 = preferences.getLong(NPSUtils.FIRST_BOOTUP_TIME, 0);
        int daysElapsed = preferences.getInt(NPSUtils.DAYS_ELAPSED, -1);
		int actualDaysElapsed = daysElapsed;
        if (rtc0 == 0 || daysElapsed == -1) {
            Log.e(TAG,
                    "NPSAlarmReceiver incrementElapsedDays(), get preferences error!!");
            return;
        }

        long currentRTC = System.currentTimeMillis();

        if (NPSUtils.DEBUG) {
            Log.i(TAG, "NPSAlarmReceiver incrementElapsedDays(), rtc0:" + rtc0);
            Log.i(TAG, "NPSAlarmReceiver incrementElapsedDays(), currentRTC:"
                    + currentRTC);
            Log.i(TAG, "NPSAlarmReceiver incrementElapsedDays(), rtc0 time:"
                    + NPSUtils.getTimeFromTimeMillis(rtc0));
            Log.i(TAG,
                    "NPSAlarmReceiver incrementElapsedDays(), currentRTC time:"
                            + NPSUtils.getTimeFromTimeMillis(currentRTC));
        }

		// less than one day, currentRTC must be greater than rtc0, this will be
		// guaranteed by NPSDataChangedReceiver::onReceive().
		if (currentRTC < rtc0) {
			Log.e(TAG, "NPSAlarmReceiver: currentRTC less than FTU time.");
			return;
		} else if (currentRTC < rtc0 + (daysElapsed + 1)
				* NPSUtils.MILLIS_A_DAY) {
			if (NPSUtils.DEBUG) {
				Log.i(TAG,
						"NPSAlarmReceiver incrementElapsedDays(), less than one day:");
			}
		} else {
			actualDaysElapsed = (int) ((currentRTC - rtc0) / NPSUtils.MILLIS_A_DAY);
			if (NPSUtils.DEBUG) {
				Log.i(TAG,
						"NPSAlarmReceiver incrementElapsedDays(), actualDaysElapsed:"
								+ actualDaysElapsed);
			}
		}
		preferences.edit().putInt(NPSUtils.DAYS_ELAPSED, actualDaysElapsed)
				.putLong(NPSUtils.LAST_RECORD_TIME, currentRTC).commit();
    }

    private boolean isTimeOutInDays() {
        int days = preferences.getInt(NPSUtils.DAYS_ELAPSED, 0);
        if (NPSUtils.DEBUG) {
            Log.i(TAG, "NPSAlarmReceiver isTimeOutInDays(), days:" + days);
        }
        if (days > NPSUtils.TIMEOUT_IN_DAYS) {
            return true;
        }
        return false;
    }

    private boolean isValidTimeslot() {
        Calendar rightNow = Calendar.getInstance();
        int hourOfDay = rightNow.get(Calendar.HOUR_OF_DAY);

        if (NPSUtils.DEBUG) {
            Log.i(TAG, "NPSAlarmReceiver isValidTimeslot(), hourOfDay:"
                    + hourOfDay);
        }
        if (hourOfDay >= 12 && hourOfDay <= 18) {
            return true;
        }
        return false;
    }

    private boolean isNotificationAllow(Context context) {
        // Allow push notification when either of the conditions are met
        // 1) Device is connected over wifi
        // 2) If no wifi, then check default data sim is not roaming
        // 3) If no default data sim assigned, then check neither of the sim are
        // roaming.
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null && connManager.getActiveNetworkInfo() != null
                && connManager.getActiveNetworkInfo().isAvailable() == true) {
            if (NPSUtils.DEBUG) {
                Log.i(TAG, "NPSAlarmReceiver isNotificationAllow(), true");
            }
            return true;
        }
        if (NPSUtils.DEBUG) {
            Log.i(TAG, "NPSAlarmReceiver isNotificationAllow(), false");
        }
        return false;
    }

}
