package com.xlythe.saolauncher.sms;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Context;
import android.os.Build;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.provider.Telephony;

public final class SmsContract {
    public static final String HOST = "com.xlythe.saolauncher";
    public static final String EXTENSION = HOST + ".smsextension";
    public static final String PERMISSION = HOST + ".permission.EXTENSION_IPC";
    public static final String AUTHORITY = EXTENSION + ".messages";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY);
    public static final String SETUP = EXTENSION + ".ui.MessageSetupActivity";
    public static final String THREADS = "threads", MESSAGES = "messages", PART = "part";
    public static final String SENDER = "sender", SELF = "self", MEMBERS = "members";
    public static final String READ_THREAD = "readThread", READ_SMS = "readSms", READ_MMS = "readMms";
    public static final String SEND = "send", REPLY = "reply", RESULT = "result", BODY = "body", TEXT = "text";

    private SmsContract() {}

    public static boolean installed(Context context) {
        PackageManager pm = context.getPackageManager();
        ProviderInfo provider = pm.resolveContentProvider(AUTHORITY, 0);
        return provider != null && EXTENSION.equals(provider.packageName)
                && pm.checkSignatures(HOST, EXTENSION) == PackageManager.SIGNATURE_MATCH;
    }

    public static String[] requiredPermissions() {
        return new String[]{Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS, Manifest.permission.RECEIVE_MMS, Manifest.permission.READ_CONTACTS};
    }

    public static boolean ready(Context context) {
        if (!installed(context) || !holdsRole(context)) return false;
        for (String permission : requiredPermissions()) {
            if (context.getPackageManager().checkPermission(permission, EXTENSION)
                    != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    public static boolean holdsRole(Context context) {
        if (EXTENSION.equals(context.getPackageName()) && Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = context.getSystemService(RoleManager.class);
            return roles != null && roles.isRoleHeld(RoleManager.ROLE_SMS);
        }
        return EXTENSION.equals(Telephony.Sms.getDefaultSmsPackage(context));
    }
}
