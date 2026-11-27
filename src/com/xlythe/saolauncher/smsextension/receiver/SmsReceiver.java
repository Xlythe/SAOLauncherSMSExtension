package com.xlythe.saolauncher.smsextension.receiver;

import com.xlythe.saolauncher.sms.SmsContract;
import android.provider.Telephony;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.xlythe.saolauncher.smsextension.BuildConfig;
import com.xlythe.saolauncher.smsextension.service.SmsReceiverService;
import com.xlythe.saolauncher.smsextension.util.Log;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())
                || Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction()))
                && SmsContract.holdsRole(context) && SmsContract.installed(context)) return;
        if(BuildConfig.DEBUG) Log.v("SMSReceiver: onReceive()");
        intent.setClass(context, SmsReceiverService.class);
        intent.putExtra("result", getResultCode());
        WakefulIntentService.sendWakefulWork(context.getApplicationContext(), intent);
    }
}
