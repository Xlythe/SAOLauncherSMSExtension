package com.xlythe.saolauncher.smsextension.ui;

import com.xlythe.saolauncher.smsextension.util.SmsMessageSender;
import android.app.Activity;
import android.os.Bundle;

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

        if(phoneNumber != null && message != null && threadId != -1l) {
            SmsMessageSender sender = new SmsMessageSender(this, new String[] { phoneNumber }, message, threadId);
            sender.sendMessage();
        }

        finish();
    }
}
