package com.nokia.feedbacktonokia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import android.util.Log;

public class NPSNetwork {
    private HttpsURLConnection urlConnection = null;
    private InputStream in = null;

    private static String APPTAG = "FeedbackToNokia";

    /*
     * Cancel the sending request, this should be called in another thread
     */
    public void cancelRequest() {
        Log.d(APPTAG, "This is in network cancel request, will close: " + in);
        try {
            if (in != null) {
                in = urlConnection.getInputStream();
                in.close();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean sendRequest(String reqUrl) {
        URL url;
        boolean ret = false;

        try {
            Log.d(APPTAG, "network send begin " + reqUrl);
            url = new URL(reqUrl);
            urlConnection = (HttpsURLConnection) url.openConnection();


            HttpsURLConnection.setFollowRedirects(true);
            urlConnection.setRequestMethod("GET");

            Log.i(APPTAG, "will get inputstream of the url.");
            in = urlConnection.getInputStream();
            Log.i(APPTAG, "the inputstream of the url is: " + in);

            Log.d(APPTAG, "Next Send status " + urlConnection.getResponseCode());

            if (urlConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                // print the result .xml from the server.
                BufferedReader bf = null;
                try {
	                Reader reader = new InputStreamReader(in, "UTF-8");
	                bf = new BufferedReader(reader);
	                String s;
	                while ((s = bf.readLine()) != null) {
	                    Log.d(APPTAG, s);
	                }
                } catch (IOException ie) {
                    ie.printStackTrace();
                } finally {
                    if (bf != null) {
                        bf.close();
                    }
                }

                // the communication with server is OK.
                ret = true;
            } else {
                ret = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    protected void finalize() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}