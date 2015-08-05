package com.nokia.feedbacktonokia;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import com.nokia.feedbacktonokia.R;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;

public class NPSActivity extends Activity implements
        CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "NPSActivity";

    private SeekBar scoreBar;
    private TextView scoreView;
    private EditText feedbackView;
    private EditText emailView;
    private ProgressDialog uploading;
    private static AlertDialog thanksAlert;
    private Button sendButton;
    private int score;
    private String emailAddress;
    private String feedback;
    private String fromWhichApp; // for phone Settings, is "Settings".
                                 // for phone notification, is "Notification".
                                 // for product/application, nothing.
    private String projectId;
    private String sourceId;
    private String backend;
    private String appVersion;
    private String genericParams;
    private String appName;

    private Context mContext;
    private Handler mHandler;
    NPSMessage npsMessage;
    private TextView privacy_info;
    private boolean hasSelectScore;
    private boolean isFromSettings;
    private boolean isFromNotification;
    private boolean isFromProduct;
    private static volatile boolean isDestroied;
    SharedPreferences.Editor preferencesEditor;
    private Set<NPSProgressDialog> cancelSet;

    private static final int SEND_OK = 0;
    private static final int SEND_ERROR = 1;
    private static final int SEND_CANCEL = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_to_nokia);
        mContext = this;
        isDestroied = false;
        NPSHandler.finished = false;
        cancelSet = new HashSet<NPSProgressDialog>();

        Intent intent = getIntent();
        String uri = intent.getDataString();

//        if (uri.endsWith("settings")) {
//            isFromSettings = true;
//        } else if (uri.endsWith("notification")) {
//            isFromNotification = true;
//        } else {
            isFromProduct = true;
//        }

        TextView recommandView = (TextView) findViewById(R.id.do_you_like);

        if (isFromSettings) {
            fromWhichApp = "Settings";
            recommandView.setText(mContext.getString(R.string.device_nps_text));
        } else if (isFromNotification) {
            fromWhichApp = "Notification";
            recommandView.setText(mContext.getString(R.string.device_nps_text));
        } else { // from product/application
            fromWhichApp = "";
            appName = intent.getStringExtra("appName");
            String s = mContext.getString(R.string.product_nps_text);
            recommandView.setText(String.format(s, appName));
            projectId = intent.getStringExtra("projectId");
            sourceId = intent.getStringExtra("sourceId");
            backend = intent.getStringExtra("backend");
            appVersion = intent.getStringExtra("appVersion");
            genericParams = intent.getStringExtra("genericParams");

            Log.d(TAG, "get data from 3rd activity: " + appName + " "
                    + fromWhichApp + " " + projectId + " " + sourceId + " "
                    + backend + " " + appVersion + " " + genericParams);
        }

        scoreBar = (SeekBar) findViewById(R.id.scorebar);
        scoreView = (TextView) findViewById(R.id.score);
        feedbackView = (EditText) findViewById(R.id.free_text_edit);
        emailView = (EditText) findViewById(R.id.email_edit);
        privacy_info = (TextView) findViewById(R.id.privacy_info);

        privacy_info.setMovementMethod(LinkMovementMethod.getInstance());

        sendButton = (Button) findViewById(R.id.send_feedback);
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // collect the user's feedback
                score = scoreBar.getProgress();
                emailAddress = emailView.getText().toString().trim();
                feedback = feedbackView.getText().toString().trim();
                feedback = feedback.replaceAll("\\s{1,}", " ");

                // show spinner on screen
                uploading = new NPSProgressDialog(NPSActivity.this);
                Log.i(TAG, "created new ProgressDialog: " + uploading);

                uploading.show();

                // to handle the sending result, OK or Error.
                mHandler = new NPSHandler(NPSActivity.this);

                // send the nps message to server in another working thread.
                new Thread() {
                    public void run() {
                        ProgressDialog local = uploading;
                        Log.i(TAG, "handle the local ProgressDialog: " + local);

                        npsMessage = new NPSMessage(mContext);
                        Message msg = new Message();
                        Log.d(TAG, "sending to NPS Server...");

                        boolean result = npsMessage.send(feedback, score,
                                emailAddress, fromWhichApp, projectId,
                                sourceId, backend, appVersion, genericParams);
                        Log.d(TAG, "sent complete. get result: " + result);

                        if (result) {
                            msg.arg1 = SEND_OK; // means OK.
                        } else {
                            boolean canceled = cancelSet.contains(local);
                            Log.i(TAG, "the cancelSet have: " + canceled + " the dialog:" + local);

                            if (canceled) {
                                msg.arg1 = SEND_CANCEL;  // for cancel
                            } else {
                                msg.arg1 = SEND_ERROR; // for error
                            }
                        }
                        cancelSet.remove(local);

                        // show user the result if succeed or error or canceled.
                        mHandler.sendMessage(msg);
                    }
                }.start();
            } // on sendButton clicked
        });

        // add check_box listener
        ((CheckBox) findViewById(R.id.check_box))
                .setOnCheckedChangeListener(this);

        // add seek bar change listener
        ((SeekBar) findViewById(R.id.scorebar))
                .setOnSeekBarChangeListener(this);

        // initialize
        hasSelectScore = false;
        preferencesEditor = PreferenceManager.getDefaultSharedPreferences(this)
                .edit();

        // look up the notification manager service, and cancel the notification
        // that we started in NPSNotifyingService
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NPSAlarmReceiver.NPS_NOTIFICATIONS);

        // hide the action bar icon
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
        }

        // hide the seekBar thumb
        scoreBar.getThumb().setAlpha(0);

        // do not show VK default
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (isFromSettings) {
            // disable the BOOT_COMPLETED receiver, and it will not be
            // invoked when the devices reboots
            int flag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            ComponentName component = new ComponentName(this,
                    NPSBootBroadcastReceiver.class);
            this.getPackageManager().setComponentEnabledSetting(
                    component, flag, PackageManager.DONT_KILL_APP);

            // disable data change receiver
            ComponentName component2 = new ComponentName(this,
                    NPSDateChangedReceiver.class);
            this.getPackageManager().setComponentEnabledSetting(
                    component2, flag, PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "NPSActivity is destroied!");
        isDestroied = true;
        if (thanksAlert != null && thanksAlert.isShowing()) {
            thanksAlert.dismiss();
        }
        super.onDestroy();
        // The activity is about to be destroyed.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked == true) {
            findViewById(R.id.email_edit).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.email_edit).setVisibility(View.INVISIBLE);
        }

    }

    public void onClickListener() {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        Log.d(TAG, "progress:" + progress + " fromUser:" + fromUser);

        int[] seekBarLocation = new int[2];
        int thumbCentralPos;

        scoreView.setText(String.valueOf(progress));

        // get view width
        int w = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);
        scoreView.measure(w, h);
        int viewWidth = scoreView.getMeasuredWidth();

        seekBar.getLocationOnScreen(seekBarLocation);
        thumbCentralPos = (int) seekBar.getX()
                + seekBar.getPaddingLeft()
                - seekBar.getThumbOffset()
                + (seekBar.getThumb().getBounds().left + seekBar.getThumb()
                        .getBounds().right) / 2;

        scoreView.setX(thumbCentralPos - (viewWidth / 2));

        scoreView.getLocationOnScreen(seekBarLocation);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "onStartTrackingTouch:");
        if (hasSelectScore == false) {
            int thumbCentralPos;

            sendButton.setEnabled(true);
            findViewById(R.id.score_selector_label).setAlpha(0);
            scoreView.setText(String.valueOf(0));
            thumbCentralPos = (int) seekBar.getX()
                    + seekBar.getPaddingLeft()
                    - seekBar.getThumbOffset()
                    + (seekBar.getThumb().getBounds().left + seekBar.getThumb()
                            .getBounds().right) / 2;
            scoreView.setX(thumbCentralPos - (scoreView.getWidth() / 2));

            seekBar.getThumb().setAlpha(255);
            scoreView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "onStopTrackingTouch:");
        if (hasSelectScore == false) {
            hasSelectScore = true;
        }
    }

    static class NPSHandler extends Handler {
        WeakReference<NPSActivity> mActivity;
        boolean succeed = false;
        static boolean finished = false;

        NPSHandler(NPSActivity act) {
            mActivity = new WeakReference<NPSActivity>(act);
        }

        public void handleMessage(Message msg) {
            Log.d(TAG, "handle message form NPS server...");
            final NPSActivity thisActivity = mActivity.get();
            if (thisActivity == null) {
                return;
            }

            if (thisActivity.uploading.isShowing()) {
                thisActivity.uploading.dismiss();
            }
            String title = "";
            String body = "";

            Context thisContext = thisActivity.mContext;
            int score = thisActivity.score;

            if (msg.arg1 == SEND_OK) {
                // show Thank you to user
                title = thisContext.getString(R.string.thank_you_title);
                if (thisActivity.isFromProduct) { // for product/software
                    body = thisContext.getString(R.string.thank_you_product);
                } else { // for phone

                    if (score >= 0 && score <= 3) {
                        body = thisContext
                                .getString(R.string.thank_you_phone_0_3);
                    } else if (score >= 4 && score <= 8) {
                        body = thisContext
                                .getString(R.string.thank_you_phone_4_8);
                    } else if (score >= 9 && score <= 10) {
                        body = thisContext
                                .getString(R.string.thank_you_phone_9_10);
                    } else {
                        // error, won't be here
                    }
                }
                succeed = true;

                if (NPSUtils.DEBUG) {
                    Log.i(TAG, "send feedback success, context:"
                            + thisActivity.mContext);
                }
            } else if (msg.arg1 == SEND_ERROR) {
                // show error information to user
                title = thisContext
                        .getString(R.string.cannot_send_feedback_title);
                body = thisContext
                        .getString(R.string.cannot_send_feedback_body);
            } else if (msg.arg1 == SEND_CANCEL) {
                // do nothing, don't show dialog to user.
                Log.d(TAG, "canceled sending when failed... won't show dialog");
                super.handleMessage(msg);
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity)
                    .setTitle(title)
                    .setMessage(body)
                    .setCancelable(false)
                    .setPositiveButton(thisContext.getString(R.string.close),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    if (succeed) {
                                        Log.d(TAG, " set finished to true!");
                                        finished = true;
                                        thisActivity.finish();
                                    }
                                }
                            });
            thanksAlert = builder.create();

            Log.d(TAG, "NPSActivity is destroied: " + NPSActivity.isDestroied + " finished: " + finished);
            synchronized(this) {
                if (!NPSActivity.isDestroied && !finished) {
                    Log.i(TAG, " before show, check finished? " + finished);
                    thanksAlert.show();
                }
            }

            super.handleMessage(msg);
        }
    }

    public class NPSProgressDialog extends ProgressDialog {

        public NPSProgressDialog(Context context) {
            super(context);

            setTitle(context.getString(R.string.status_message_while_sending));
            setProgressStyle(ProgressDialog.STYLE_SPINNER);
            setCancelable(false);

            setOnKeyListener(new OnKeyListener() {

                // when press back key, it will also cancel the sending process.
                public boolean onKey(DialogInterface dialog, int keyCode,
                        KeyEvent event) {
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK:
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            Log.d(TAG, "KEYCODE_BACK clicked!");
                            dismiss();
                            new Thread() {
                                public void run() {
                                    cancelSet.add(NPSProgressDialog.this);
                                    npsMessage.cancel();
                                }
                            }.start();
                            return true;
                        }
                    }
                    return false;
                }
            });

            String cancel = context.getString(R.string.cancel);
            setButton(AlertDialog.BUTTON_POSITIVE, cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "Cancel sending...");
                            if (npsMessage != null) {
                                new Thread() {
                                    public void run() {
                                        Log.i(TAG, "add " + NPSProgressDialog.this + " to cancelSet.");
                                        cancelSet.add(NPSProgressDialog.this);
                                        npsMessage.cancel();
                                    }
                                }.start();
                            }
                        }
                    });
        }

        public NPSProgressDialog(Context context, int theme) {
            super(context, theme);
        }

        @Override
        public void setView(View view) {
            super.setView(view);

            View v = view.findViewById(android.R.id.progress);
            LayoutParams l = (LayoutParams) v.getLayoutParams();
            l.gravity = Gravity.CENTER_HORIZONTAL;
            l.weight = 1.0f;
            v.setLayoutParams(l);

            v = view.findViewById(android.R.id.message);
            v.setVisibility(View.GONE);

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            setResult(0);
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
