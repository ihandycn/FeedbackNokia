package com.nokia.feedbacktonokia;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager; 
//import android.os.SystemProperties;
import android.util.Log;

public class NPSUtils {
    public static final boolean DEBUG = true;

    private Context mContext;

    private static String APPTAG = "FeedbackToNokia";
    
    // for testing, MUST set it to true!
    private boolean isTestMode = false;
 
    private String npsurl = "";
    private String mSourceId = ""; // this id should get from NPS admin tool
    private String mProjectId = ""; // this id should get from NPS admin tool
    
    private String mFeedback = "";
    private String mEmail = "";
    private int mScore;
    private String mSource = "";
    private String mPlatformVersion = "1.0";
    private String mRMCode = "";
    private String mReleaseVersion = "";
    private String mNPSVersion = "1.0";
    private String mMcc = "";
    private String mMnc = "";
    private String mIMEI = "";
    private String mFirstBoot = "";
    private String mMcuSwUpdateDate = ""; 
    private String mTimeStamp = "";
    private String mAppVersion = "";
    private String mUniqueDeviceId = "";
    private int mDaysUsed = 0;

    public static final String FIRST_BOOTUP_FLAG = "is_first_bootup";
    public static final String FIRST_BOOTUP_TIME = "first_bootup_time";
    public static final String LAST_RECORD_TIME = "last_record_time";
    public static final String DAYS_ELAPSED = "days_elapsed";
    public static final long MILLIS_A_DAY = 24*60*60*1000L;
    public static final int TIMEOUT_IN_DAYS = 14;

    public NPSUtils(Context context) {
        mContext = context;
    }

    public String getURL(String feedback, int score,
            String email, String source, String projectId, String sourceId,
            String backend, String appVersion, String genericParams) {   	
        

        mFeedback = feedback;
        mEmail = email;
        mScore = score;
        mSource = source;

        boolean ret = getConfigInfor();
        if (!ret)
            Log.e(APPTAG, "Get language error");

        if (projectId != null && sourceId != null && !projectId.isEmpty()
                && !sourceId.isEmpty()) {
            mProjectId = projectId;
            mSourceId = sourceId;

            if (appVersion != null)
                mAppVersion = appVersion;
        }
        
        String url = npsurl;

        try {

            url += "projectId=" + mProjectId + "&sourceId=" + mSourceId
                    + "&score=" + mScore 
                    + "&country=" + URLEncoder.encode(mMcc, "UTF-8") 
                    + "&operator=" + URLEncoder.encode(mMnc, "UTF-8") 
                    + "&sw=" + URLEncoder.encode(mReleaseVersion, "UTF-8")
                    + "&hw=" + URLEncoder.encode(mPlatformVersion, "UTF-8")
                    + "&version=" + URLEncoder.encode(mNPSVersion, "UTF-8")
                    + "&dateEntered=" + URLEncoder.encode(mTimeStamp, "UTF-8")
                    + "&daysUsed=" + mDaysUsed;


            if (mFeedback != null)
                url += "&feedback=" + URLEncoder.encode(mFeedback, "UTF-8");

            if (mEmail != null)
                url += "&email=" + URLEncoder.encode(mEmail, "UTF-8");

            if (mRMCode != null)
                url += "&device=" + URLEncoder.encode(mRMCode, "UTF-8");

            if (mFirstBoot != null)
                url += "&firstBoot=" + URLEncoder.encode(mFirstBoot, "UTF-8");

            if (mIMEI != null)
                url += "&imei=" + URLEncoder.encode(mIMEI, "UTF-8");

            if (mSource != null)
                url += "&source=" + URLEncoder.encode(mSource, "UTF-8");

            if (mUniqueDeviceId != null &&  !mUniqueDeviceId.isEmpty())
            	url += "&deviceUniqueID=" + URLEncoder.encode(mUniqueDeviceId, "UTF-8");

            if (mMcuSwUpdateDate != null)
                url += "&mcuSwUpdateDate="
                        + URLEncoder.encode(mMcuSwUpdateDate, "UTF-8");
            
            if(mAppVersion != null && !mAppVersion.isEmpty())
            	url += "&releaseVersion="
            			+ URLEncoder.encode(mAppVersion, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return url;
    }

    private boolean getConfigInfor() {
    	
		if (isTestMode) {
			// please use it when you do testing!
			npsurl = "https://cpqqa.nokia.com/www/api.php?";
			mSourceId = "28efda15fd7ef19a68bc3f4dd8abf977";  
			mProjectId = "2533";  
		} else {
			// used for the real product condition, do NOT use it for testing!
			npsurl = "https://cpq.nokia.com/www/api.php?";
			mSourceId = "fc7f26db722e2f4272986616d2e99872";
			mProjectId = "2312";
		}

//        mRMCode = SystemProperties.get("ro.product.name", "");
//        String appVersoin =
//                SystemProperties.get("apps.setting.product.swversion","");
//        if(appVersoin != null)
//            mReleaseVersion = appVersoin;


        String tmFormat = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(tmFormat);
        mTimeStamp = sdf.format(new Date(System.currentTimeMillis()));

        TelephonyManager telManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = telManager.getSubscriberId();
        if (imsi != null && imsi.length() > 5) {
            mMcc = imsi.substring(0, 3);
            mMnc = imsi.substring(3, 5);
        }

        mIMEI = telManager.getDeviceId();

        Log.d(APPTAG, "get IMEI \"" + mIMEI + "\""); 
        
        if(mIMEI != null)
    	{
        	mIMEI = getSha1String(mIMEI);
    	}


        if (android.os.Build.DISPLAY != null)
            mPlatformVersion = android.os.Build.DISPLAY;

        Date cur = new Date(android.os.Build.TIME);

        if (cur != null)
            mMcuSwUpdateDate = sdf.format(cur);

        String fbt = getFTUTime();
        if(fbt != null){
            mFirstBoot = fbt;
            
            /*
             * if first boot time is exists then use it as sw update date
             * because of this date will be changed after flashed/reset phone.
             */
            mMcuSwUpdateDate = fbt;
            
            long curMil = new Date().getTime(); 
            
            long dfbt = getFTUTimeByMills();
            Log.d(APPTAG, "Current Milli " + curMil + " fbt " + dfbt );

            mDaysUsed = (int) Math.floor((curMil - dfbt) / (24 * 60 * 60 * 1000));             
            
        }
        
        String m_androidId = Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
        Log.d(APPTAG, "get ANDROID_ID \"" + m_androidId + "\""); 
        
        if(m_androidId != null){        
        	
        	mUniqueDeviceId = m_androidId;
        	
        }
        
        return true;
    }

    public static String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate);
    }

    public static String getTimeFromTimeMillis(long millis) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date curDate = new Date(millis);
        return formatter.format(curDate);
    }

    public String getFTUTime() { 
        long rtc0 = getFTUTimeByMills();
        return getTimeFromTimeMillis(rtc0);
    }
    
    public long getFTUTimeByMills() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        return preferences.getLong(FIRST_BOOTUP_TIME, System.currentTimeMillis());
    }
    
	private String getSha1String(String inputStr) {
		
		String ret = "";
		
		try {
			// IMEI need hash to sha1 format
			MessageDigest digester = MessageDigest.getInstance("SHA1");

			byte[] hash;

			hash = digester.digest(inputStr.getBytes());

			StringBuffer sb = new StringBuffer();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}

			ret = sb.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return ret;
	}
}
