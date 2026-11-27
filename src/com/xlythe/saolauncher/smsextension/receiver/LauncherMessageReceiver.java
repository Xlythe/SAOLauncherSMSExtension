package com.xlythe.saolauncher.smsextension.receiver;

import android.content.Context;
import android.content.Intent;

import com.xlythe.saolauncher.sms.SmsContract;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextReceiver;

/** Forward the package-local TextManager notification after it persists an SMS or MMS. */
public class LauncherMessageReceiver extends TextReceiver {
    @Override public void onMessageReceived(Context context, Text text) {
        if (text != null && SmsContract.installed(context) && SmsContract.holdsRole(context)) {
            context.sendBroadcast(new Intent(ACTION_TEXT_RECEIVED)
                    .setClassName(SmsContract.HOST, SmsContract.HOST + ".receiver.SMSReceiver")
                    .putExtra(SmsContract.TEXT, text.toBytes()), SmsContract.PERMISSION);
        }
    }
}
