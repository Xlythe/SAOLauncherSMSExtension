package com.xlythe.saolauncher.smsextension.ui;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;

import com.xlythe.saolauncher.sms.SmsContract;

import java.util.ArrayList;

public class MessageSetupActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) return;
        if (SmsContract.holdsRole(this)) {
            requestAccess();
        } else if (Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles == null || !roles.isRoleAvailable(RoleManager.ROLE_SMS)) {
                finish();
                return;
            }
            startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_SMS), 1);
        } else {
            startActivityForResult(new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                    .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName()), 1);
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 1 && SmsContract.holdsRole(this)) requestAccess();
        else finish();
    }

    private void requestAccess() {
        ArrayList<String> missing = new ArrayList<>();
        for (String permission : SmsContract.requiredPermissions()) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) missing.add(permission);
        }
        if (missing.isEmpty()) {
            setResult(RESULT_OK);
            finish();
        } else {
            requestPermissions(missing.toArray(new String[0]), 2);
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] names, int[] grants) {
        super.onRequestPermissionsResult(request, names, grants);
        if (request == 2 && grants.length > 0) {
            for (int grant : grants) {
                if (grant != PackageManager.PERMISSION_GRANTED) { finish(); return; }
            }
            setResult(RESULT_OK);
        }
        finish();
    }
}
