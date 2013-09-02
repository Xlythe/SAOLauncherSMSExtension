package com.xlythe.saolauncher.smsextension.receiver;

import com.xlythe.saolauncher.smsextension.BuildConfig;
import com.xlythe.saolauncher.smsextension.service.SmsReceiverService;
import com.xlythe.saolauncher.smsextension.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v("SMSReceiver: onReceive()");
        intent.setClass(context, SmsReceiverService.class);
        intent.putExtra("result", getResultCode());
        WakefulIntentService.sendWakefulWork(context.getApplicationContext(), intent);
    }
}