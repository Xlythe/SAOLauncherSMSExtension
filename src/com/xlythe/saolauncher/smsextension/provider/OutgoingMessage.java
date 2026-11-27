package com.xlythe.saolauncher.smsextension.provider;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.content.FileProvider;
import com.xlythe.saolauncher.smsextension.util.SmsMessageSender;
import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.PduBody;
import com.xlythe.textmanager.text.pdu.PduComposer;
import com.xlythe.textmanager.text.pdu.PduPart;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.SendReq;
import com.xlythe.textmanager.text.util.CharacterSets;
import com.xlythe.textmanager.text.util.EncodedStringValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** A single-recipient reply is SMS. Group replies stay in the original MMS conversation. */
public final class OutgoingMessage extends BroadcastReceiver {
    public static boolean send(Context context, Set<String> recipients, String body, long threadId) {
        if (recipients.size() == 1) {
            return new SmsMessageSender(context, recipients.toArray(new String[0]), body, threadId).sendMessage();
        }

        SendReq pdu = new SendReq();
        for (String number : recipients) pdu.addTo(new EncodedStringValue(number));
        pdu.setDate(System.currentTimeMillis() / 1000);
        PduPart part = new PduPart();
        part.setName("text.txt".getBytes(StandardCharsets.US_ASCII));
        part.setContentType("text/plain".getBytes(StandardCharsets.US_ASCII));
        part.setCharset(CharacterSets.UTF_8);
        part.setData(body.getBytes(StandardCharsets.UTF_8));
        PduBody parts = new PduBody();
        parts.addPart(part);
        pdu.setBody(parts);

        File file = null;
        Uri outbox = null;
        try {
            File directory = new File(context.getCacheDir(), "mms-transfers");
            if (!directory.isDirectory() && !directory.mkdirs()) return false;
            file = File.createTempFile("send-", ".pdu", directory);
            try (FileOutputStream stream = new FileOutputStream(file)) {
                stream.write(new PduComposer(context, pdu).make());
            }
            outbox = PduPersister.getPduPersister(context).persist(pdu, Telephony.Mms.Outbox.CONTENT_URI, true, true, null);
            Intent callback = new Intent(context, OutgoingMessage.class).setData(outbox).putExtra("file", file.getName());
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".transfers", file);
            SmsManager.getDefault().sendMultimediaMessage(context, uri, null, null,
                    PendingIntent.getBroadcast(context, 0, callback, PendingIntent.FLAG_IMMUTABLE));
            return true;
        } catch (MmsException | IOException | RuntimeException unavailable) {
            if (file != null) file.delete();
            if (outbox != null) complete(context, outbox, false);
            Log.w("Messages", "MMS could not be queued", unavailable);
            return false;
        }
    }

    @Override public void onReceive(Context context, Intent intent) {
        complete(context, intent.getData(), getResultCode() == Activity.RESULT_OK);
        new File(new File(context.getCacheDir(), "mms-transfers"), intent.getStringExtra("file")).delete();
    }

    private static void complete(Context context, Uri row, boolean sent) {
        ContentValues state = new ContentValues();
        state.put(Telephony.Mms.MESSAGE_BOX, sent ? Telephony.Mms.MESSAGE_BOX_SENT : Telephony.Mms.MESSAGE_BOX_FAILED);
        context.getContentResolver().update(row, state, null, null);
    }
}
