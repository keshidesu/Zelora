package com.zelora.app.util;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import com.zelora.app.R;
import com.zelora.app.model.Order;
import com.zelora.app.ui.OrdersActivity;

import java.util.List;

// notification
public final class NotificationHelper {
    private static final String CHANNEL = "order_status";
    private static final String PREFS = "order_seen";

    private NotificationHelper() { }

    public static void checkOrders(Context ctx, int userId, List<Order> orders) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        for (Order o : orders) {
            String key = "u" + userId + "_o" + o.id;
            String last = sp.getString(key, null);
            if (last != null && !last.equals(o.status)) {
                show(ctx, o);
            }
            ed.putString(key, o.status);
        }
        ed.apply();
    }

    private static void show(Context ctx, Order o) {
        if (Build.VERSION.SDK_INT >= 33
                && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Order updates", NotificationManager.IMPORTANCE_DEFAULT));

        PendingIntent open = PendingIntent.getActivity(ctx, o.id,
                new Intent(ctx, OrdersActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_cart)
                .setContentTitle("Order " + o.code)
                .setContentText(message(o.status))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        nm.notify(o.id, n);
    }

    private static String message(String status) {
        switch (status) {
            case Order.PREPARING: return "The seller is preparing your order.";
            case Order.PACKED:    return "The seller packed your order.";
            case Order.OUT:       return "A rider is on the way with your order!";
            case Order.DELIVERED: return "Delivered. Don't forget to rate your items.";
            case Order.CANCELLED: return "Your order was cancelled.";
            default:              return "Your order is now: " + status;
        }
    }
}
