package com.nokia.feedbacktonokia;

import com.nokia.feedbacktonokia.R;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

public class NokiaPrivacyPolicyActivity extends Activity implements
        View.OnClickListener {
    private TextView privacy_info_page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nokia_privacy_policy);

        privacy_info_page = (TextView) findViewById(R.id.privacy_info_page);
        privacy_info_page.setMovementMethod(LinkMovementMethod.getInstance());

        // add check_box listener
        ((Button) findViewById(R.id.privacy_info_page_ok))
                .setOnClickListener(this);

        // hide the action bar icon
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            // actionBar.setIcon(R.drawable.fake_action_icon);
            actionBar.setDisplayShowHomeEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.privacy_info_page_ok:
            setResult(0);
            finish();
            break;
        default:
            break;
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
