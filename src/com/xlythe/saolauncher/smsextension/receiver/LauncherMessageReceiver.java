package com.xlythe.saolauncher.smsextension.receiver;

import android.content.Context;
import android.content.Intent;

import com.xlythe.saolauncher.sms.SmsContract;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextReceiver;

/** TextReceiver persists SMS_DELIVER in the role holder before invoking this callback. */
public class LauncherMessageReceiver extends TextReceiver {
    @Override public void onMessageReceived(Context context, Text text) {
        forward(context, text);
    }

    public static void forward(Context context, Text text) {
        if (text != null && SmsContract.installed(context) && SmsContract.holdsRole(context)) {
            context.sendBroadcast(new Intent(ACTION_TEXT_RECEIVED)
                    .setClassName(SmsContract.HOST, SmsContract.HOST + ".receiver.SMSReceiver")
                    .putExtra(SmsContract.TEXT, text.toBytes()), SmsContract.PERMISSION);
        }
    }
}
