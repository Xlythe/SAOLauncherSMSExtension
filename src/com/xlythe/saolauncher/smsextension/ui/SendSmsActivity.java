package com.xlythe.saolauncher.smsextension.ui;

import android.app.Activity;
import android.os.Bundle;

import com.xlythe.saolauncher.smsextension.R;

public class SendSmsActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            runOperation();
        } else if (bundle == null) {
            requestPermissions(new String[] {android.Manifest.permission.SEND_SMS}, 1);
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] names, int[] grants) {
        super.onRequestPermissionsResult(request, names, grants);
        if (request == 1 && grants.length > 0 && grants[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) runOperation();
        else finish();
    }

    private void runOperation() {
        String number = getIntent().getStringExtra("sms_number");
        String body = getIntent().getStringExtra("sms_msg");
        if (!android.text.TextUtils.isEmpty(number) && !android.text.TextUtils.isEmpty(body)) {
            android.telephony.SmsManager sms = android.telephony.SmsManager.getDefault();
            try {
                java.util.ArrayList<String> parts = sms.divideMessage(body);
                if (parts.size() == 1) sms.sendTextMessage(number, null, body, null, null);
                else sms.sendMultipartTextMessage(number, null, parts, null, null);
            } catch (IllegalArgumentException | SecurityException | UnsupportedOperationException unavailable) {
                android.widget.Toast.makeText(this, R.string.quickreply_failed, android.widget.Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
}
