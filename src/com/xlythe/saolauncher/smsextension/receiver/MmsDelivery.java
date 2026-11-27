package com.xlythe.saolauncher.smsextension.receiver;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.content.FileProvider;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.GenericPdu;
import com.xlythe.textmanager.text.pdu.NotificationInd;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.RetrieveConf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Give each carrier download its own URI, inbox row and immutable completion callback. */
public class MmsDelivery extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        byte[] bytes = intent.getByteArrayExtra("data");
        if (bytes == null) return;
        Uri row = null;
        File file = null;
        try {
            GenericPdu pdu = new PduParser(bytes, true).parse();
            if (!(pdu instanceof NotificationInd)) return;
            byte[] location = ((NotificationInd) pdu).getContentLocation();
            if (location == null) return;
            row = PduPersister.getPduPersister(context).persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
            update(context, row, false);
            File directory = new File(context.getCacheDir(), "mms-transfers");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("MMS cache unavailable");
            file = File.createTempFile("receive-", ".pdu", directory);
            Uri transfer = FileProvider.getUriForFile(context, context.getPackageName() + ".transfers", file);
            Intent result = new Intent(context, DownloadResult.class).setData(row).putExtra("file", file.getName());
            int sub = intent.getIntExtra("subscription", -1);
            SmsManager manager = sub >= 0 ? SmsManager.getSmsManagerForSubscriptionId(sub) : SmsManager.getDefault();
            manager.downloadMultimediaMessage(context, new String(location, StandardCharsets.UTF_8), transfer, null,
                    PendingIntent.getBroadcast(context, 0, result, PendingIntent.FLAG_IMMUTABLE));
        } catch (MmsException | IOException | RuntimeException failed) {
            if (row != null) update(context, row, true);
            if (file != null) file.delete();
            Log.w("Messages", "Unable to start MMS download", failed);
        }
    }

    private static void update(Context context, Uri row, boolean failed) {
        ContentValues state = new ContentValues();
        state.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000);
        if (failed) state.put(Telephony.Mms.STATUS, Telephony.Sms.STATUS_FAILED);
        context.getContentResolver().update(row, state, null, null);
    }

    public static class DownloadResult extends BroadcastReceiver {
        private static final Executor IO = Executors.newSingleThreadExecutor();

        @Override public void onReceive(Context context, Intent intent) {
            boolean success = getResultCode() == Activity.RESULT_OK;
            PendingResult pending = goAsync();
            IO.execute(() -> {
                File file = new File(new File(context.getCacheDir(), "mms-transfers"), intent.getStringExtra("file"));
                Uri row = intent.getData();
                try {
                    if (!success) { update(context, row, true); return; }
                    try (FileInputStream input = new FileInputStream(file);
                         ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            if (bytes.size() + count > 16 * 1024 * 1024) throw new IOException("Oversized MMS response");
                            bytes.write(buffer, 0, count);
                        }
                        GenericPdu message = new PduParser(bytes.toByteArray(), true).parse();
                        if (!(message instanceof RetrieveConf)) throw new IOException("Invalid MMS response");
                        PduPersister.getPduPersister(context).persist(message, row, true, true, null);
                    }
                    update(context, row, false);
                    try (Cursor c = context.getContentResolver().query(row, new String[]{"thread_id"}, null, null, null)) {
                        if (c == null || !c.moveToFirst()) return;
                        try (Text.TextCursor messages = TextManager.getInstance(context).getMessageCursor(c.getString(0))) {
                            while (messages.moveToNext()) {
                                Text text = messages.getText();
                                if (text.isMms() && text.getMmsId() == ContentUris.parseId(row)) {
                                    LauncherMessageReceiver.forward(context, text);
                                    break;
                                }
                            }
                        }
                    }
                } catch (MmsException | IOException | RuntimeException failed) {
                    update(context, row, true);
                    Log.w("Messages", "Unable to receive MMS", failed);
                } finally {
                    file.delete();
                    pending.finish();
                }
            });
        }
    }
}
