package com.w3engineers.unicef.util.lib.boxpassword;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BoxPasswordTest {
    private String defaultPass = "123456789101";
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getContext();
    }

    @Test
    public void boxPasswordEventTest(){

        addDelay();

        BoxPassword password =new BoxPassword(mContext);


        password.setOnClickListener(v -> {
            addDelay();
            password.setText(defaultPass);
        });

        addDelay();

        password.setText(" 123 567 ");

        addDelay();
    }

    private void addDelay() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}