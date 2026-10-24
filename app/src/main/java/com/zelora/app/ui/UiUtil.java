package com.zelora.app.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.zelora.app.R;

// ui
public final class UiUtil {
    private UiUtil() {}

    public static void toast(Context c, String message) {
        Toast.makeText(c, message, Toast.LENGTH_SHORT).show();
    }

    public static void hideKeyboard(Activity a) {
        View v = a.getCurrentFocus();
        if (v == null) return;
        InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static void setupPasswordToggle(final EditText field, final ImageView eye) {
        eye.setOnClickListener(v -> {
            boolean hidden = (field.getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0;
            int selection = field.getSelectionStart();
            Typeface font = field.getTypeface();
            if (hidden) {
                field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                eye.setImageResource(R.drawable.ic_eye);
            } else {
                field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                eye.setImageResource(R.drawable.ic_eye_off);
            }
            field.setTypeface(font);
            field.setSelection(Math.max(selection, 0));
        });
    }

    public static void setLink(Context c, TextView view, String plain, String link, final Runnable onClick) {
        String full = plain + link;
        SpannableString s = new SpannableString(full);
        int start = plain.length();
        s.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                onClick.run();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setUnderlineText(false);
            }
        }, start, full.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new StyleSpan(Typeface.BOLD), start, full.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(c, R.color.text_primary)), start, full.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(s);
        view.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static void setupToolbar(final Activity a, String title) {
        a.findViewById(R.id.btnBack).setOnClickListener(v -> a.finish());
        ((TextView) a.findViewById(R.id.tvTitle)).setText(title);
    }

    public static int dp(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    public static String formatDate(String utc) {
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date d = in.parse(utc);
            return new java.text.SimpleDateFormat("MMM d, yyyy  h:mm a", java.util.Locale.getDefault()).format(d);
        } catch (Exception e) {
            return utc;
        }
    }

    public static String timeAgo(String utc) {
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            long mins = (System.currentTimeMillis() - in.parse(utc).getTime()) / 60000;
            if (mins < 1) return "Just now";
            if (mins < 60) return mins + " min ago";
            if (mins < 24 * 60) return (mins / 60) + " h ago";
            return formatDate(utc);
        } catch (Exception e) {
            return utc == null ? "" : utc;
        }
    }

    public static String greeting() {
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        return h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
    }
}
