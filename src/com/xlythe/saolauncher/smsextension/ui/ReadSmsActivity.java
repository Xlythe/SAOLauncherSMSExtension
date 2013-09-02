package com.xlythe.saolauncher.smsextension.ui;

import android.app.Activity;
import android.os.Bundle;

import com.xlythe.saolauncher.smsextension.provider.SmsMmsMessage;

public class ReadSmsActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        long messageId = -1l;
        int messageType = -1;
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            if(extras.containsKey("sms_msg_id")) {
                messageId = extras.getLong("sms_msg_id");
            }
            if(extras.containsKey("sms_msg_type")) {
                messageType = extras.getInt("sms_msg_type");
            }
        }

        if(messageId != -1l && messageType != -1) {
            SmsMmsMessage message = new SmsMmsMessage(this, messageId, -1l, -1l, null, -1, messageType);
            message.setMessageRead();
        }

        finish();
    }
}
