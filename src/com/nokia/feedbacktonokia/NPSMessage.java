package com.nokia.feedbacktonokia;

import android.content.Context;
import android.util.Log;

public class NPSMessage {
    private final static String APPTAG = "FeedbackToNokia";

    private NPSUtils mNpsUtils;
    private NPSNetwork mNNetwork;
    private boolean isCanceled = false;

    public NPSMessage(Context context) {
        mNNetwork = new NPSNetwork();
        mNpsUtils = new NPSUtils(context);
    }

    public boolean send(String feedback, int score, String email,
            String source, String projectId, String sourceId, String backend,
            String appVersion, String genericParams) {
        boolean status = false;
        String firstUrl;

        if (!isCanceled) {
            firstUrl = mNpsUtils.getURL(feedback, score, email, source, projectId,
                    sourceId, backend, appVersion, genericParams);
        } else {
            return false;
        }

        if (!isCanceled) {
            status = mNNetwork.sendRequest(firstUrl);
        } else {
            Log.d(APPTAG, "send message canceled ");
            status = false;
            isCanceled = false;
        }

        return status;
    }

    public void cancel() {
        isCanceled = true;
        mNNetwork.cancelRequest();
    }

}