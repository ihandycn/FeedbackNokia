package com.nokia.feedbacktonokia.test;

import com.nokia.feedbacktonokia.NPSActivity;
import com.nokia.feedbacktonokia.NPSIntroActivity;
import com.nokia.feedbacktonokia.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.ViewAsserts;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class NPSIntroActivityTest extends
        ActivityInstrumentationTestCase2<NPSIntroActivity> {

    private NPSIntroActivity mNPSIntroActivity;
    private Button mButtonOK;
    private Button mButtonLater;
    private static final int TIMEOUT_IN_MS = 5000;

    public NPSIntroActivityTest() {
        super(NPSIntroActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        //Sets the initial touch mode for the Activity under test. This must be called before
        //getActivity()
        setActivityInitialTouchMode(true);
        
        mNPSIntroActivity = getActivity();
        mButtonOK = (Button) mNPSIntroActivity.findViewById(R.id.intro_page_ok);
        mButtonLater = (Button) mNPSIntroActivity.findViewById(R.id.intro_page_later);
    }

    public void testPreconditions() {
        // Try to add a message to add context to your assertions. These
        // messages will be shown if
        // a tests fails and make it easy to understand why a test failed
        assertNotNull("mNPSIntroActivity is null", mNPSIntroActivity);
        assertNotNull("mButtonOK is null", mButtonOK);
        assertNotNull("mButtonLater is null", mButtonLater);
    }

    /**
     * Tests the correctness of the initial text.
     */
    public void testCase01_ButtonLabelText() {
        final String expected = mNPSIntroActivity.getString(R.string.OK);
        final String actual = mButtonOK.getText().toString();
        assertEquals("NPSIntroActivity contains wrong text", expected, actual);

        final String expected2 = mNPSIntroActivity.getString(R.string.Later);
        final String actual2 = mButtonLater.getText().toString();
        assertEquals("NPSIntroActivity contains wrong text 2", expected2, actual2);
    }
    
    @MediumTest
    public void testCase02_Button_layout() {
        //Retrieve the top-level window decor view
        final View decorView = mNPSIntroActivity.getWindow().getDecorView();

        //Verify that the 2 Buttons are on screen
        ViewAsserts.assertOnScreen(decorView, mButtonOK);
        ViewAsserts.assertOnScreen(decorView, mButtonLater);

        //Verify width and heights
        final ViewGroup.LayoutParams layoutParams = mButtonOK.getLayoutParams();
        assertNotNull(layoutParams);
        assertEquals(layoutParams.width, 280); // 280px
        assertEquals(layoutParams.height, 52);
        
        final ViewGroup.LayoutParams layoutParams2 = mButtonLater.getLayoutParams();
        assertNotNull(layoutParams2);
        assertEquals(layoutParams2.width, 280); // 280px
        assertEquals(layoutParams2.height, 52);
    }

    @MediumTest
    public void testCase03_Button_Click() {
        
        //Create and add an ActivityMonitor to monitor interaction between the system and the
        //NPSActivity
        Instrumentation.ActivityMonitor receiverActivityMonitor = getInstrumentation()
                .addMonitor(NPSActivity.class.getName(), null, false);

        //Wait until all events from the MainHandler's queue are processed
        getInstrumentation().waitForIdleSync();

        //Click on the OK Button to start NPSActivity
        TouchUtils.clickView(this, mButtonOK);

        //Wait until NPSActivity was launched and get a reference to it.
        NPSActivity receiverActivity = (NPSActivity) receiverActivityMonitor
                .waitForActivityWithTimeout(TIMEOUT_IN_MS);
        //Verify that ReceiverActivity was started
        assertNotNull("ReceiverActivity is null", receiverActivity);
        assertEquals("Monitor for ReceiverActivity has not been called", 1,
                receiverActivityMonitor.getHits());
        assertEquals("Activity is of wrong type", NPSActivity.class,
                receiverActivity.getClass());

        //Unregister monitor for ReceiverActivity
        getInstrumentation().removeMonitor(receiverActivityMonitor);
    }
}
