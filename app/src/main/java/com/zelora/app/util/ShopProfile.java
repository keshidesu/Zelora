package com.zelora.app.util;

import android.content.Context;
import android.content.SharedPreferences;

// profile
public final class ShopProfile {
    private ShopProfile() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("staff_profile", Context.MODE_PRIVATE);
    }

    public static String shopName(Context c, int sellerId) {
        return prefs(c).getString("shop_" + sellerId, "My Shop");
    }

    public static void setShopName(Context c, int sellerId, String name) {
        prefs(c).edit().putString("shop_" + sellerId, name.trim()).apply();
    }

    public static boolean isOnline(Context c, int riderId) {
        return prefs(c).getBoolean("online_" + riderId, true);
    }

    public static void setOnline(Context c, int riderId, boolean online) {
        prefs(c).edit().putBoolean("online_" + riderId, online).apply();
    }
}
