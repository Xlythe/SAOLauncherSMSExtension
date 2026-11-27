package com.xlythe.saolauncher.smsextension.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.Telephony;
import android.text.TextUtils;

import com.xlythe.saolauncher.sms.SmsContract;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** The launcher can access messages only while this extension holds the SMS role. */
public class LauncherMessages extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    private long enter() {
        Context context = getContext();
        context.enforceCallingPermission(SmsContract.PERMISSION, "Launcher signature required");
        if (!SmsContract.HOST.equals(getCallingPackage())) {
            throw new SecurityException("Message IPC is restricted to the launcher");
        }
        long token = Binder.clearCallingIdentity();
        try {
            if (!SmsContract.holdsRole(context)) throw new SecurityException("SMS role required");
            return token;
        } catch (RuntimeException failure) {
            Binder.restoreCallingIdentity(token);
            throw failure;
        }
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        long caller = enter();
        try {
            TextManager manager = TextManager.getInstance(getContext());
            MatrixCursor result = new MatrixCursor(new String[]{"message"});
            if (uri.equals(SmsContract.URI.buildUpon().appendPath(SmsContract.THREADS).build())) {
                try (Thread.ThreadCursor rows = manager.getThreadCursor()) {
                    rows.moveToPosition(-1);
                    while (rows.moveToNext()) add(result, rows.getThread());
                }
            } else if (isItem(uri, SmsContract.MESSAGES)) {
                try (Text.TextCursor rows = manager.getMessageCursor(Long.toString(ContentUris.parseId(uri)))) {
                    while (rows.moveToNext()) add(result, rows.getText());
                }
            } else {
                throw new IllegalArgumentException("Unknown message query");
            }
            return result;
        } finally { Binder.restoreCallingIdentity(caller); }
    }

    private static void add(MatrixCursor cursor, Parcelable value) {
        Parcel parcel = Parcel.obtain();
        try {
            value.writeToParcel(parcel, 0);
            cursor.addRow(new Object[]{parcel.marshall()});
        } finally { parcel.recycle(); }
    }

    private static boolean isItem(Uri uri, String path) {
        return uri.getPathSegments().size() == 2 && path.equals(uri.getPathSegments().get(0))
                && TextUtils.isDigitsOnly(uri.getLastPathSegment());
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        long caller = enter();
        try {
            if (!"r".equals(mode) || !isItem(uri, SmsContract.PART)) throw new FileNotFoundException();
            return getContext().getContentResolver().openFileDescriptor(
                    ContentUris.withAppendedId(Uri.parse("content://mms/part"), ContentUris.parseId(uri)), "r");
        } finally { Binder.restoreCallingIdentity(caller); }
    }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        long caller = enter();
        try {
            TextManager manager = TextManager.getInstance(getContext());
            Bundle result = new Bundle();
            if (extras != null) extras.setClassLoader(Text.class.getClassLoader());
            switch (method) {
                case SmsContract.SELF:
                    result.putParcelable(SmsContract.RESULT, manager.getSelf());
                    break;
                case SmsContract.SENDER:
                    result.putParcelable(SmsContract.RESULT, manager.getSender(Text.fromBytes(extras.getByteArray(SmsContract.TEXT))).get());
                    break;
                case SmsContract.MEMBERS:
                    result.putParcelableArrayList(SmsContract.RESULT,
                            new ArrayList<>(manager.getMembersExceptMe(Text.fromBytes(extras.getByteArray(SmsContract.TEXT))).get()));
                    break;
                case SmsContract.READ_THREAD:
                    markRead(Telephony.Sms.CONTENT_URI, "thread_id", arg);
                    markRead(Telephony.Mms.CONTENT_URI, "thread_id", arg);
                    break;
                case SmsContract.READ_SMS:
                    markRead(Telephony.Sms.CONTENT_URI, "_id", arg);
                    break;
                case SmsContract.READ_MMS:
                    markRead(Telephony.Mms.CONTENT_URI, "_id", arg);
                    break;
                case SmsContract.SEND:
                case SmsContract.REPLY:
                    String body = extras.getString(SmsContract.BODY);
                    if (TextUtils.isEmpty(body) || TextUtils.isEmpty(arg)) break;
                    Set<String> addresses;
                    long threadId;
                    if (SmsContract.REPLY.equals(method)) {
                        threadId = Long.parseLong(arg);
                        Thread conversation = manager.getThread(threadId).get();
                        if (conversation == null || conversation.getLatestMessage() == null) break;
                        Set<Contact> contacts = manager.getMembersExceptMe(conversation.getLatestMessage()).get();
                        addresses = new HashSet<>();
                        for (Contact contact : contacts) {
                            if (!TextUtils.isEmpty(contact.getNumber())) addresses.add(contact.getNumber());
                        }
                    } else {
                        addresses = Collections.singleton(arg);
                        threadId = Telephony.Threads.getOrCreateThreadId(getContext(), arg);
                    }
                    if (addresses.isEmpty()) break;
                    result.putBoolean(SmsContract.RESULT, OutgoingMessage.send(getContext(), addresses, body, threadId));
                    break;
                default: throw new IllegalArgumentException("Unknown message operation");
            }
            return result;
        } finally { Binder.restoreCallingIdentity(caller); }
    }

    private void markRead(Uri table, String key, String id) {
        ContentValues values = new ContentValues();
        values.put("read", 1);
        getContext().getContentResolver().update(table, values, key + "=? AND read=0",
                new String[]{Long.toString(Long.parseLong(id))});
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String where, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String where, String[] args) { throw new UnsupportedOperationException(); }
}
