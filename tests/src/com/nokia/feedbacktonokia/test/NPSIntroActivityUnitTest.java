package com.nokia.feedbacktonokia.test;

import com.nokia.feedbacktonokia.NPSIntroActivity;
import com.nokia.feedbacktonokia.R;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.Button;

public class NPSIntroActivityUnitTest extends
        ActivityUnitTestCase<NPSIntroActivity> {

    private Intent mLaunchIntent;

    public NPSIntroActivityUnitTest() {
        super(NPSIntroActivity.class);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Create an intent to launch target Activity
        mLaunchIntent = new Intent(getInstrumentation().getTargetContext(),
                NPSIntroActivity.class);
    }

    public void testPreconditions() {
        NPSIntroActivity mNPSIntroActivity;
        Button mButtonOK;
        Button mButtonLater;
        // Start the activity under test in isolation, without values for
        // savedInstanceState and
        // lastNonConfigurationInstance
        startActivity(mLaunchIntent, null, null);

        mNPSIntroActivity = getActivity();
        mButtonOK = (Button) mNPSIntroActivity.findViewById(R.id.intro_page_ok);
        mButtonLater = (Button) mNPSIntroActivity
                .findViewById(R.id.intro_page_later);

        assertNotNull("mNPSIntroActivity is null", mNPSIntroActivity);
        assertNotNull("mButtonOK is null", mButtonOK);
        assertNotNull("mButtonLater is null", mButtonLater);
    }

    @MediumTest
    public void testCase01_NextActivityWasLaunchedWithIntent() {
        startActivity(mLaunchIntent, null, null);
        final Button mButtonOK = (Button) getActivity().findViewById(
                R.id.intro_page_ok);
        // Because this is an isolated ActivityUnitTestCase we have to directly
        // click the
        // button from code
        mButtonOK.performClick();

        // Get the intent for the next started activity
        final Intent launchIntent = getStartedActivityIntent();
        // Verify the intent was not null.
        assertNotNull("Intent was null", launchIntent);
        // Verify that NPSIntroActivity was finished after button click
        assertTrue(isFinishCalled());

        final String dataString = launchIntent.getDataString();
        // Verify that data string was added to the intent
        assertEquals("intent data wrong", dataString,
                "nps://launch_feedbacktonokia/notification");
    }

    @MediumTest
    public void testCase02_ActivityWasClosedAfterClickLaterButton() {
        startActivity(mLaunchIntent, null, null);
        final Button mButtonLater = (Button) getActivity().findViewById(
                R.id.intro_page_later);
        // Because this is an isolated ActivityUnitTestCase we have to directly
        // click the
        // button from code
        mButtonLater.performClick();

        // Verify that NPSIntroActivity was finished after button click
        assertTrue(isFinishCalled());
    }
}
