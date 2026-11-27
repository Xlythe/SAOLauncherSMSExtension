package com.xlythe.saolauncher.smsextension.ui;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.xlythe.saolauncher.smsextension.provider.SmsMmsMessage;

public class ReadSmsActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            runOperation();
        } else if (bundle == null) {
            requestPermissions(new String[] {Manifest.permission.READ_SMS}, 1);
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] names, int[] grants) {
        super.onRequestPermissionsResult(request, names, grants);
        if (request == 1 && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) runOperation();
        else finish();
    }

    private void runOperation() {

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
