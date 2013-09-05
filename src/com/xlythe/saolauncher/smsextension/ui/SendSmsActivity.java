package com.xlythe.saolauncher.smsextension.ui;

import android.app.Activity;
import android.os.Bundle;

import com.xlythe.saolauncher.smsextension.util.SmsMessageSender;

public class SendSmsActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        String phoneNumber = null;
        String message = null;
        long threadId = -1l;
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            if(extras.containsKey("sms_number")) {
                phoneNumber = extras.getString("sms_number");
            }
            if(extras.containsKey("sms_msg")) {
                message = extras.getString("sms_msg");
            }
            if(extras.containsKey("sms_thread_id")) {
                threadId = extras.getLong("sms_thread_id");
            }
        }

        if(phoneNumber != null && message != null) {
            SmsMessageSender sender = new SmsMessageSender(this, new String[] { phoneNumber }, message, threadId);
            sender.sendMessage();
        }

        finish();
    }
}
