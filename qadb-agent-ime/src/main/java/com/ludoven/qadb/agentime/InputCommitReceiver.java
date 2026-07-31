package com.ludoven.qadb.agentime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import java.nio.charset.StandardCharsets;

public final class InputCommitReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String encoded = intent.getStringExtra("text64");
        if (encoded == null || encoded.length() == 0) {
            setResultCode(2);
            setResultData("missing text64");
            return;
        }
        try {
            byte[] decoded = Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP);
            String text = new String(decoded, StandardCharsets.UTF_8);
            boolean committed = QadbInputMethodService.commitText(text);
            setResultCode(committed ? 0 : 3);
            setResultData(committed ? "committed" : "input connection unavailable");
        } catch (Throwable error) {
            setResultCode(4);
            setResultData(error.getClass().getSimpleName());
        }
    }
}
