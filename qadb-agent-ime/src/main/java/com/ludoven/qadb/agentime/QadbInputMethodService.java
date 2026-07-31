package com.ludoven.qadb.agentime;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;
import java.lang.ref.WeakReference;

public final class QadbInputMethodService extends InputMethodService {
    private static WeakReference<QadbInputMethodService> active =
        new WeakReference<QadbInputMethodService>(null);

    @Override
    public void onCreate() {
        super.onCreate();
        active = new WeakReference<QadbInputMethodService>(this);
    }

    @Override
    public void onDestroy() {
        QadbInputMethodService current = active.get();
        if (current == this) active.clear();
        super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
        return new View(this);
    }

    static boolean commitText(String text) {
        QadbInputMethodService service = active.get();
        if (service == null) return false;
        InputConnection connection = service.getCurrentInputConnection();
        return connection != null && connection.commitText(text, 1);
    }
}
