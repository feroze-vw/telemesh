package com.w3engineers.unicef.util.helper;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.w3engineers.unicef.telemesh.ui.aboutus.AboutUsActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CommonUtilTest {
    private Context mContext;
    public UiDevice mDevice = UiDevice.getInstance(getInstrumentation());

    @Rule
    public ActivityTestRule<AboutUsActivity> rule = new ActivityTestRule<>(AboutUsActivity.class);

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    @UiThreadTest
    public void permissionPopupTest() {
        addDelay(500);
        CommonUtil commonUtil = new CommonUtil();
        commonUtil.showPermissionPopUp(rule.getActivity());

        addDelay(5000);

        CommonUtil.dismissDialog();

    }

    @Test
    @UiThreadTest
    public void showLocationPermissionPopupTest() {
        addDelay(500);
        CommonUtil.showGpsOrLocationOffPopup(rule.getActivity());

        addDelay(2000);

        CommonUtil.dismissDialog();
    }

    private void addDelay(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}