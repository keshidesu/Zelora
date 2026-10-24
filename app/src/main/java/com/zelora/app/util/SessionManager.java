package com.zelora.app.util;

import android.content.Context;
import android.content.SharedPreferences;

// session
public class SessionManager {
    private static final String PREFS = "zelora_session";
    private static final String KEY_USER_ID = "user_id";
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void login(int userId) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply();
    }

    public void logout() {
        prefs.edit().remove(KEY_USER_ID).apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public boolean isLoggedIn() {
        return getUserId() != -1;
    }
}
