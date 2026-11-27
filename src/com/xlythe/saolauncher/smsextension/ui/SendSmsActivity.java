package com.xlythe.saolauncher.smsextension.ui;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.xlythe.saolauncher.smsextension.R;
import com.xlythe.saolauncher.smsextension.util.SmsMessageSender;

public class SendSmsActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            runOperation();
        } else if (bundle == null) {
            requestPermissions(new String[] {Manifest.permission.SEND_SMS}, 1);
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] names, int[] grants) {
        super.onRequestPermissionsResult(request, names, grants);
        if (request == 1 && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) runOperation();
        else finish();
    }

    private void runOperation() {
        String number = getIntent().getStringExtra("sms_number");
        String body = getIntent().getStringExtra("sms_msg");
        if (!TextUtils.isEmpty(number) && !TextUtils.isEmpty(body)) {
            if (!SmsMessageSender.send(this, body, number)) {
                Toast.makeText(this, R.string.quickreply_failed, Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
}
