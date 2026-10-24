package com.zelora.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.zelora.app.model.Dispute;

import java.util.ArrayList;
import java.util.List;

// dispute
public class DisputeDao {
    private final DatabaseHelper helper;

    public DisputeDao(Context context) {
        helper = DatabaseHelper.get(context);
    }

    public boolean create(int userId, int orderId, String reason, String details) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor c = db.rawQuery("SELECT 1 FROM orders WHERE id=? AND user_id=?"
                            + " AND status IN ('Packed','Out for Delivery','Delivered')"
                            + " AND NOT EXISTS (SELECT 1 FROM disputes WHERE order_id = orders.id AND status='Open')",
                    new String[]{String.valueOf(orderId), String.valueOf(userId)})) {
                if (!c.moveToFirst()) return false;
            }
            ContentValues v = new ContentValues();
            v.put("order_id", orderId);
            v.put("user_id", userId);
            v.put("reason", reason);
            v.put("details", details == null ? "" : details.trim());
            db.insertOrThrow("disputes", null, v);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public List<Dispute> getAll() {
        List<Dispute> out = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT d.id, d.order_id, o.order_code, u.full_name, d.reason, d.details, d.status, d.admin_note, d.created_at"
                        + " FROM disputes d JOIN orders o ON o.id = d.order_id JOIN users u ON u.id = d.user_id"
                        + " ORDER BY CASE d.status WHEN 'Open' THEN 0 ELSE 1 END, d.id DESC", null)) {
            while (c.moveToNext()) {
                Dispute d = new Dispute();
                d.id = c.getInt(0);
                d.orderId = c.getInt(1);
                d.orderCode = c.getString(2);
                d.customerName = c.getString(3);
                d.reason = c.getString(4);
                d.details = c.getString(5);
                d.status = c.getString(6);
                d.adminNote = c.getString(7);
                d.createdAt = c.getString(8);
                out.add(d);
            }
        }
        return out;
    }

    public boolean decide(int disputeId, boolean resolved, String note, boolean refund) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            int orderId = -1;
            try (Cursor c = db.rawQuery("SELECT order_id FROM disputes WHERE id=? AND status='Open'",
                    new String[]{String.valueOf(disputeId)})) {
                if (!c.moveToFirst()) return false;
                orderId = c.getInt(0);
            }
            ContentValues v = new ContentValues();
            v.put("status", resolved ? "Resolved" : "Rejected");
            v.put("admin_note", note == null ? "" : note.trim());
            db.update("disputes", v, "id=?", new String[]{String.valueOf(disputeId)});
            if (resolved && refund) {
                db.execSQL("UPDATE payments SET status='REFUNDED' WHERE order_id=? AND status = 'PAID'",
                        new Object[]{orderId});
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }
}
