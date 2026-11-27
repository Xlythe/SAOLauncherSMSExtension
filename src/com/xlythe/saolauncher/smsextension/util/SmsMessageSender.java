/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.xlythe.saolauncher.smsextension.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.text.TextUtils;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.xlythe.saolauncher.smsextension.R;
import com.xlythe.textmanager.text.TextManager;

public final class SmsMessageSender {
    private SmsMessageSender() {}

    public static boolean send(Context context, String body, String... addresses) {
        return send(context, body, SubscriptionManager.INVALID_SUBSCRIPTION_ID, addresses);
    }

    public static boolean send(Context context, String body, int subscription, String... addresses) {
        if (TextUtils.isEmpty(body) || addresses.length == 0) return false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            TextManager.getInstance(context).send(body)
                    .usingSubscription(subscription)
                    .separateSmsParts(prefs.getBoolean(context.getString(R.string.pref_split_message_key), false))
                    .requestSmsDeliveryReports(prefs.getBoolean(context.getString(R.string.pref_delivery_report_key), false))
                    .to(addresses);
            return true;
        } catch (IllegalArgumentException | IllegalStateException | SecurityException unavailable) {
            Log.w("Messages", "Could not queue message", unavailable);
            return false;
        }
    }

    public static final String MESSAGING_PACKAGE_NAME = "com.android.mms";
    public static final String MESSAGING_RECEIVER_CLASS_NAME = MESSAGING_PACKAGE_NAME + ".transaction.SmsReceiver";
    public static final String MESSAGING_CONVO_CLASS_NAME = "com.android.mms.ui.ConversationList";
    public static final String MESSAGING_COMPOSE_CLASS_NAME = "com.android.mms.ui.ComposeMessageActivity";
    public static final String SAMSUNG_MESSAGING_COMPOSE_CLASS_NAME = "com.android.mms.ui.ConversationComposer";
    public static final String[] MESSAGING_APP_ACTIVITIES = { MESSAGING_CONVO_CLASS_NAME, MESSAGING_COMPOSE_CLASS_NAME, SAMSUNG_MESSAGING_COMPOSE_CLASS_NAME, };

    // Handles outstanding send callbacks created by earlier extension versions.
    public static boolean moveMessageToFolder(Context context, Uri uri, int folder) {
        if(uri == null) {
            return false;
        }

        boolean markAsUnread = false;
        boolean markAsRead = false;
        switch(folder) {
        case Telephony.Sms.MESSAGE_TYPE_INBOX:
        case Telephony.Sms.MESSAGE_TYPE_DRAFT:
            break;
        case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
        case Telephony.Sms.MESSAGE_TYPE_SENT:
            markAsRead = true;
            break;
        case Telephony.Sms.MESSAGE_TYPE_FAILED:
        case Telephony.Sms.MESSAGE_TYPE_QUEUED:
            markAsUnread = true;
            break;
        default:
            return false;
        }

        ContentValues values = new ContentValues(2);

        values.put(Telephony.Sms.TYPE, folder);
        if(markAsUnread) {
            values.put(Telephony.Sms.READ, Integer.valueOf(0));
        }
        else if(markAsRead) {
            values.put(Telephony.Sms.READ, Integer.valueOf(1));
        }

        int result = 0;

        try {
            result = context.getContentResolver().update(uri, values, null, null);
        }
        catch(Exception e) {

        }

        return 1 == result;

    }

}
