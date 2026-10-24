package com.zelora.app.ui;

import android.app.Activity;
import android.content.Intent;

import com.zelora.app.model.User;

// auth
public final class Nav {
    private Nav() {}

    public static Class<?> homeFor(User user) {
        switch (user.role) {
            case User.SELLER: return SellerActivity.class;
            case User.ADMIN:  return AdminActivity.class;
            case User.RIDER:  return RiderActivity.class;
            default:          return MainActivity.class;
        }
    }

    public static void openHome(Activity from, User user) {
        Intent i = new Intent(from, homeFor(user));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(i);
        from.finish();
    }

    public static void toLogin(Activity from) {
        Intent i = new Intent(from, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(i);
        from.finish();
    }
}
