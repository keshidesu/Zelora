package com.zelora.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.zelora.app.model.CartItem;

import java.util.ArrayList;
import java.util.List;

// cart
public class CartDao {
    public static final int MAX_QTY = 10;
    private final DatabaseHelper helper;

    public CartDao(Context context) {
        helper = DatabaseHelper.get(context);
    }

    public void add(int userId, int productId, String color, int qty) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            int existingId = -1, existingQty = 0;
            try (Cursor c = db.rawQuery(
                    "SELECT id, quantity FROM cart_items WHERE user_id=? AND product_id=? AND color=?",
                    new String[]{String.valueOf(userId), String.valueOf(productId), color})) {
                if (c.moveToFirst()) {
                    existingId = c.getInt(0);
                    existingQty = c.getInt(1);
                }
            }
            if (existingId == -1) {
                ContentValues v = new ContentValues();
                v.put("user_id", userId);
                v.put("product_id", productId);
                v.put("color", color);
                v.put("quantity", Math.min(qty, MAX_QTY));
                db.insert("cart_items", null, v);
            } else {
                ContentValues v = new ContentValues();
                v.put("quantity", Math.min(existingQty + qty, MAX_QTY));
                db.update("cart_items", v, "id=?", new String[]{String.valueOf(existingId)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<CartItem> getItems(int userId) {
        List<CartItem> out = new ArrayList<>();
        String sql = "SELECT ci.id, ci.color, ci.quantity, " + ProductDao.COLUMNS
                + " FROM cart_items ci JOIN products p ON p.id = ci.product_id"
                + " WHERE ci.user_id = ? AND p.active = 1 ORDER BY ci.id DESC";
        try (Cursor c = helper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) {
                out.add(new CartItem(c.getInt(0), ProductDao.fromCursor(c, 3), c.getString(1), c.getInt(2)));
            }
        }
        return out;
    }

    public int count(int userId) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(quantity),0) FROM cart_items WHERE user_id=?",
                new String[]{String.valueOf(userId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public void updateQuantity(int cartId, int qty) {
        ContentValues v = new ContentValues();
        v.put("quantity", Math.max(1, Math.min(qty, MAX_QTY)));
        helper.getWritableDatabase().update("cart_items", v, "id=?", new String[]{String.valueOf(cartId)});
    }

    public void remove(int cartId) {
        helper.getWritableDatabase().delete("cart_items", "id=?", new String[]{String.valueOf(cartId)});
    }
}
