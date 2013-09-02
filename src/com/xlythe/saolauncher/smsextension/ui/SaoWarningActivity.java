package com.xlythe.saolauncher.smsextension.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.xlythe.saolauncher.smsextension.R;

public class SaoWarningActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.warning_sao_missing_title);
        builder.setMessage(R.string.warning_sao_missing_message);
        builder.setPositiveButton(R.string.warning_sao_missing_button, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.xlythe.saolauncher"));
                startActivity(intent);
                finish();
            }
        });
        builder.setCancelable(true);
        builder.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                finish();
            }
        });
        builder.create().show();
    }
}
