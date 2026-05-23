package com.example.frontend_bookingcare.ui.common;

import android.os.Build;
import android.os.LocaleList;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Keeps EditText IME composition stable for Vietnamese (Telex/VNI/Gboard).
 * Avoid CAP_SENTENCES / CAP_WORDS — they break diacritic composition mid-word.
 */
public final class UnicodeInputHelper {

    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    private UnicodeInputHelper() {
    }

    public static void enableSingleLineText(@Nullable EditText field) {
        if (field == null) return;
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        hintVietnameseIme(field);
    }

    public static void enableMultilineText(@Nullable EditText field) {
        if (field == null) return;
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        hintVietnameseIme(field);
    }

    public static void enableChatInput(@Nullable EditText field) {
        if (field == null) return;
        field.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        hintVietnameseIme(field);
        field.setImeOptions(EditorInfo.IME_ACTION_SEND);
        field.setMaxLines(4);
    }

    private static void hintVietnameseIme(@Nullable EditText field) {
        if (field == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        field.setImeHintLocales(new LocaleList(VI));
    }

    /** True while Telex/VNI is still composing a diacritic — avoid filtering UI in TextWatchers. */
    public static boolean isImeComposing(@Nullable Editable text) {
        if (text == null) return false;
        Object[] spans = text.getSpans(0, text.length(), Object.class);
        for (Object span : spans) {
            if ((text.getSpanFlags(span) & Spanned.SPAN_COMPOSING) != 0) {
                return true;
            }
        }
        return false;
    }
}
