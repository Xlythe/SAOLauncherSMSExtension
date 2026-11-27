package com.xlythe.saolauncher.smsextension.service;

import android.app.IntentService;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.xlythe.saolauncher.sms.SmsContract;
import com.xlythe.saolauncher.smsextension.provider.OutgoingMessage;
import com.xlythe.textmanager.text.util.MessageUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Android's reply-with-a-message action when declining a call. */
public class QuickResponseService extends IntentService {
    public QuickResponseService() { super("QuickResponse"); }

    @Override protected void onHandleIntent(Intent intent) {
        if (intent == null || !TelephonyManager.ACTION_RESPOND_VIA_MESSAGE.equals(intent.getAction())
                || !SmsContract.holdsRole(this)) return;
        String[] numbers = MessageUtils.getRecipients(intent);
        String body = MessageUtils.getBody(intent);
        if (numbers == null || numbers.length == 0 || TextUtils.isEmpty(body)) return;
        Set<String> addresses = new HashSet<>(Arrays.asList(numbers));
        OutgoingMessage.send(this, addresses, body, Telephony.Threads.getOrCreateThreadId(this, addresses));
    }
}
