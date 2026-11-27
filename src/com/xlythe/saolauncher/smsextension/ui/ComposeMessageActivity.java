package com.xlythe.saolauncher.smsextension.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.xlythe.saolauncher.sms.SmsContract;
import com.xlythe.saolauncher.smsextension.R;
import com.xlythe.saolauncher.smsextension.provider.OutgoingMessage;
import com.xlythe.textmanager.text.util.MessageUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** SENDTO opens a composer. Only its Send button submits a message. */
public class ComposeMessageActivity extends Activity {
    private EditText to;
    private EditText message;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String[] numbers = MessageUtils.getRecipients(getIntent());
        String body = MessageUtils.getBody(getIntent());
        if (state == null && numbers != null && numbers.length == 1 && SmsContract.ready(this)) {
            try {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", numbers[0], null))
                        .putExtra(Intent.EXTRA_TEXT, body)
                        .setClassName(SmsContract.HOST, SmsContract.HOST + ".activity.ComposeSMSActivity"));
                finish();
                return;
            } catch (ActivityNotFoundException | SecurityException unavailable) {
                // The launcher may have been removed while opening the composer.
            }
        }
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        to = new EditText(this);
        to.setHint(R.string.message_recipients);
        to.setInputType(InputType.TYPE_CLASS_PHONE);
        to.setText(state != null ? state.getString("to") : numbers == null ? "" : TextUtils.join(";", numbers));
        fields.addView(to);
        message = new EditText(this);
        message.setHint(R.string.message_body);
        message.setText(state != null ? state.getString("body") : body);
        fields.addView(message);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(fields)
                .setNegativeButton(android.R.string.cancel, (d, w) -> finish())
                .setPositiveButton(R.string.quickreply_send_button, null).create();
        dialog.setOnCancelListener(d -> finish());
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!SmsContract.holdsRole(this) || checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(new Intent(this, MessageSetupActivity.class), 1);
                return;
            }
            Set<String> recipients = new HashSet<>(Arrays.asList(to.getText().toString().trim().split("\\s*[;,]\\s*")));
            recipients.remove("");
            if (recipients.isEmpty() || message.length() == 0) return;
            if (OutgoingMessage.send(this, recipients, message.getText().toString(),
                    Telephony.Threads.getOrCreateThreadId(this, recipients))) finish();
            else Toast.makeText(this, R.string.quickreply_failed, Toast.LENGTH_SHORT).show();
        });
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (to != null) state.putString("to", to.getText().toString());
        if (message != null) state.putString("body", message.getText().toString());
    }
}
